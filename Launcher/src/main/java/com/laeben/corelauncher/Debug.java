package com.laeben.corelauncher;

import javafx.animation.*;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Circle;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class Debug {
    public static class WarpDownloadOverlay extends Pane {

        // Mesh yoğunluğu (ne kadar yüksekse o kadar pürüzsüz, ama CPU yükü artar)
        private static final int GRID_COLS = 24;
        private static final int GRID_ROWS = 24;

        // Kütleçekim sabiti (piksel²/saniye) – ne kadar büyükse çekim o kadar hızlı
        private static final double G = 30000;
        // Girdap (swirl) katsayısı – ne kadar büyükse dönüş o kadar hızlı
        private static final double SWIRL_K = 20000;
        // Sıfıra bölünmeyi önleyen küçük değer
        private static final double EPS = 1e-3;
        // Maksimum animasyon süresi (saniye)
        private static final double MAX_DURATION = 99999.0;

        public WarpDownloadOverlay() {
            // Overlay tıklamaları geçirsin, odaklanmasın
            setMouseTransparent(true);
            setPickOnBounds(false);
        }

        /**
         * Warp animasyonunu başlatır.
         *
         * @param card         Animasyona girecek kart düğümü (Node)
         * @param downloadIcon Sağ üstteki indirme simgesi (tekillik noktası)
         * @param onComplete   Animasyon tamamlanınca çağrılacak callback
         */
        public void startWarp(Node card, Node downloadIcon, Runnable onComplete) {
            // 1. Kartın sahne koordinatlarındaki sınırlarını al
            Bounds cardBounds = card.localToScene(card.getBoundsInLocal());
            double width = cardBounds.getWidth();
            double height = cardBounds.getHeight();
            if (width <= 0 || height <= 0) {
                if (onComplete != null) onComplete.run();
                return;
            }

            // 2. Kartın anlık görüntüsünü (snapshot) al
            WritableImage snapshot = card.snapshot(
                    new SnapshotParameters(),
                    new WritableImage((int) Math.ceil(width), (int) Math.ceil(height))
            );

            // Kartı gizle (layout bozulmaz, sadece görünmez olur)
            card.setVisible(false);

            // 3. Tekillik noktasının sahne koordinatlarını al
            Bounds iconBounds = downloadIcon.localToScene(downloadIcon.getBoundsInLocal());


            // 4. Mesh grid oluştur
            TriangleMesh mesh = buildMesh(width, height);
            PhongMaterial material = new PhongMaterial();
            material.setSelfIlluminationMap(snapshot);  // Işık gerektirmeden görüntüyü göster
            material.setDiffuseColor(Color.WHITE);
            material.setSpecularColor(Color.TRANSPARENT);

            MeshView meshView = new MeshView(mesh);
            meshView.setMaterial(material);
            meshView.setCullFace(CullFace.NONE);        // Her iki yüzü de göster
            meshView.setTranslateX(cardBounds.getMinX());
            meshView.setTranslateY(cardBounds.getMinY());

            getChildren().add(meshView);

            Point2D singularity = new Point2D(
                    750,
                    750
            );

            // Tekillik noktasını kartın lokal koordinatlarına çevir
            double sx = singularity.getX() - cardBounds.getMinX();
            double sy = singularity.getY() - cardBounds.getMinY();

            // 5. Animasyon zamanlayıcısını başlat
            final double[] elapsed = {0};
            final boolean[] finished = {false};
            AnimationTimer timer = new AnimationTimer() {
                private long lastTime = -1;

                @Override
                public void handle(long now) {
                    if (lastTime == -1) {
                        lastTime = now;
                        return;
                    }
                    double dt = (now - lastTime) / 1e9; // nanosaniye -> saniye
                    lastTime = now;
                    elapsed[0] += dt;



                    updateMeshPoints(mesh, width, height, sx, sy, elapsed[0]);

                    if (elapsed[0] >= MAX_DURATION && !finished[0]) {
                        finished[0] = true;
                        stop();
                        getChildren().remove(meshView);

                        // Şok dalgası ve indirme yüzdesini göster
                        playShockwave(singularity);
                        showDownloadProgress(singularity);

                        if (onComplete != null) {
                            onComplete.run();
                        }
                    }
                }
            };
            timer.start();
        }

        /** Kart boyutlarına göre üçgen mesh oluşturur. */
        private TriangleMesh buildMesh(double width, double height) {
            TriangleMesh mesh = new TriangleMesh();

            int numVertices = (GRID_COLS + 1) * (GRID_ROWS + 1);
            float[] points = new float[numVertices * 3];
            float[] texCoords = new float[numVertices * 2];
            int[] faces = new int[GRID_COLS * GRID_ROWS * 2 * 6]; // her hücre 2 üçgen

            int pointIndex = 0;
            int texIndex = 0;
            for (int j = 0; j <= GRID_ROWS; j++) {
                for (int i = 0; i <= GRID_COLS; i++) {
                    float x = (float) (i * width / GRID_COLS);
                    float y = (float) (j * height / GRID_ROWS);

                    points[pointIndex++] = x;
                    points[pointIndex++] = y;
                    points[pointIndex++] = 0; // 2D olduğu için z = 0

                    texCoords[texIndex++] = (float) i / GRID_COLS;
                    texCoords[texIndex++] = (float) j / GRID_ROWS;
                }
            }

            int faceIndex = 0;
            for (int j = 0; j < GRID_ROWS; j++) {
                for (int i = 0; i < GRID_COLS; i++) {
                    int p00 = vertexIndex(i, j);
                    int p10 = vertexIndex(i + 1, j);
                    int p01 = vertexIndex(i, j + 1);
                    int p11 = vertexIndex(i + 1, j + 1);

                    // Üçgen 1
                    faces[faceIndex++] = p00;
                    faces[faceIndex++] = p00;
                    faces[faceIndex++] = p10;
                    faces[faceIndex++] = p10;
                    faces[faceIndex++] = p01;
                    faces[faceIndex++] = p01;

                    // Üçgen 2
                    faces[faceIndex++] = p10;
                    faces[faceIndex++] = p10;
                    faces[faceIndex++] = p11;
                    faces[faceIndex++] = p11;
                    faces[faceIndex++] = p01;
                    faces[faceIndex++] = p01;
                }
            }

            mesh.getPoints().setAll(points);
            mesh.getTexCoords().setAll(texCoords);
            mesh.getFaces().setAll(faces);

            return mesh;
        }

        private int vertexIndex(int i, int j) {
            return j * (GRID_COLS + 1) + i;
        }

        /**
         * Her karede vertex'leri kütleçekim, spagettileşme ve girdap etkisiyle günceller.
         */
        private void updateMeshPoints(TriangleMesh mesh, double width, double height,
                                      double sx, double sy, double t) {
            float[] points = new float[(GRID_COLS + 1) * (GRID_ROWS + 1) * 3];
            int pointIndex = 0;

            for (int j = 0; j <= GRID_ROWS; j++) {
                for (int i = 0; i <= GRID_COLS; i++) {
                    double x0 = i * width / GRID_COLS;
                    double y0 = j * height / GRID_ROWS;

                    double dx = x0 - sx;
                    double dy = y0 - sy;
                    double r0 = Math.sqrt(dx * dx + dy * dy);

                    if (r0 < EPS) {
                        // Tekillik noktasındaki vertex zaten merkezde
                        points[pointIndex++] = (float) sx;
                        points[pointIndex++] = (float) sy;
                        points[pointIndex++] = 0;
                        continue;
                    }

                    // Kütleçekim: yakın vertex'ler daha hızlı çekilir.
                    // arrival süresi r0^2 ile orantılı olduğu için Newton'un kütleçekim yasasına benzer.
                    double arrival = G / r0;
                    double progress = Math.min(1.0, Math.pow(t / arrival, 1.5));
                    double r = r0 * (1.0 - progress);  // Spagettileşme: farklı hızlar kartı uzatır

                    // Açısal teğetsel dönüş (swirl/vortex)
                    double theta = SWIRL_K * t / (r0 * r0 + EPS);
                    double angle = Math.atan2(dy, dx) + theta;

                    double newX = sx + r * Math.cos(angle);
                    double newY = sy + r * Math.sin(angle);

                    points[pointIndex++] = (float) newX;
                    points[pointIndex++] = (float) newY;
                    points[pointIndex++] = 0;
                }
            }
            mesh.getPoints().set(0, points, 0, points.length);
        }

        /** Tekillik noktasında kütleçekimsel şok dalgası (pulse/bloom) oluşturur. */
        private void playShockwave(Point2D center) {
            Circle shockwave = new Circle(center.getX(), center.getY(), 0, Color.rgb(255, 255, 255, 0.8));
            shockwave.setStroke(Color.WHITE);
            shockwave.setStrokeWidth(2);
            shockwave.setFill(Color.TRANSPARENT);
            shockwave.setMouseTransparent(true);
            getChildren().add(shockwave);

            Timeline timeline = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(shockwave.radiusProperty(), 0),
                            new KeyValue(shockwave.opacityProperty(), 0.8)
                    ),
                    new KeyFrame(Duration.seconds(0.8),
                            new KeyValue(shockwave.radiusProperty(), 100),
                            new KeyValue(shockwave.opacityProperty(), 0)
                    )
            );
            timeline.setOnFinished(e -> getChildren().remove(shockwave));
            timeline.play();
        }

        /** İndirme yüzdesini tekillik noktasının yanında gösterir. */
        private void showDownloadProgress(Point2D center) {
            Label progressLabel = new Label("0%");
            progressLabel.setTextFill(Color.WHITE);
            progressLabel.setStyle("-fx-background-color: rgba(0,0,0,0.6); " +
                    "-fx-background-radius: 5; -fx-padding: 4 8;");
            progressLabel.setLayoutX(center.getX() + 20);
            progressLabel.setLayoutY(center.getY() - 15);
            progressLabel.setMouseTransparent(true);
            getChildren().add(progressLabel);

            final int[] count = {0};
            Timeline progressTimeline = new Timeline(new KeyFrame(Duration.millis(20), e -> {
                count[0]++;
                if (count[0] <= 100) {
                    progressLabel.setText(count[0] + "%");
                }
            }));
            progressTimeline.setCycleCount(100);  // 100 * 20ms = 2 saniye
            progressTimeline.setOnFinished(e -> getChildren().remove(progressLabel));
            progressTimeline.play();
        }
    }

    public static class DownloadCard extends StackPane {

        private final VBox content;
        private final Label downloadIcon;
        private final Button downloadButton;
        private Runnable onDownload;
        private boolean downloaded = false;

        public DownloadCard(String title, String description, int index) {
            setPrefSize(200, 150);
            setMaxSize(200, 150);
            setStyle("-fx-background-color: #2a2a3e; -fx-background-radius: 10; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.5), 10, 0, 0, 4);");

            // Sağ üstteki indirme simgesi (tekillik noktası olacak)
            downloadIcon = new Label("⬇");
            downloadIcon.setStyle("-fx-background-color: #4a4a6a; -fx-background-radius: 50%; " +
                    "-fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 5;");
            downloadIcon.setPrefSize(30, 30);
            downloadIcon.setAlignment(Pos.CENTER);
            StackPane.setAlignment(downloadIcon, Pos.TOP_RIGHT);
            StackPane.setMargin(downloadIcon, new Insets(8));

            // Kart içeriği
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold;");
            Label descLabel = new Label(description);
            descLabel.setStyle("-fx-text-fill: #b0b0c0; -fx-font-size: 12px;");
            descLabel.setWrapText(true);

            downloadButton = new Button("İndir");
            downloadButton.setStyle("-fx-background-color: #5a5a8a; -fx-text-fill: white; " +
                    "-fx-background-radius: 5;");
            downloadButton.setOnAction(e -> {
                if (onDownload != null && !downloaded) {
                    onDownload.run();
                }
            });

            content = new VBox(8, titleLabel, descLabel, downloadButton);
            content.setAlignment(Pos.CENTER_LEFT);
            content.setPadding(new Insets(10));
            content.setMaxWidth(Double.MAX_VALUE);

            getChildren().addAll(content, downloadIcon);
        }

        /** Sağ üstteki indirme simgesini döndürür (tekillik noktası). */
        public Node getDownloadIcon() {
            return downloadIcon;
        }

        /** İndirme butonuna basıldığında çalışacak callback. */
        public void setOnDownload(Runnable action) {
            this.onDownload = action;
        }

        /** İndirme tamamlandığında kartın görünümünü günceller. */
        public void setDownloaded(boolean downloaded) {
            this.downloaded = downloaded;
            if (downloaded) {
                downloadIcon.setText("✓");
                downloadIcon.setStyle("-fx-background-color: #4a8a4a; -fx-background-radius: 50%; " +
                        "-fx-text-fill: white; -fx-font-size: 18px; -fx-padding: 5;");
                downloadButton.setText("Tamamlandı");
                downloadButton.setDisable(true);
            }
        }
    }

    public static final boolean DEBUG = false;
    public static final boolean DEBUG_UI = false;

    public static void run(){
        //var profile = Profiler.getProfiler().getProfile("FTB Continuumm");
        //var shortcutPath = Path.begin(java.nio.file.Path.of("C:/Users/furka/Desktop/java"));
        //var k = GsonUtil.DEFAULT_GSON.fromJson(js, Instructor.class);
    }
    public static void runUI() throws FileNotFoundException {
        // --- 1. ANA DÜZEN (LAYOUT) ---
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e2e;");

        // Kartların bulunduğu akış paneli
        FlowPane cardPane = new FlowPane(20, 20);
        cardPane.setPadding(new Insets(20));
        cardPane.setStyle("-fx-background-color: transparent;");

        // Overlay katmanı (tüm warp animasyonları burada)
        WarpDownloadOverlay overlay = new WarpDownloadOverlay();
        overlay.setMouseTransparent(true);   // Tıklamaları alta geçir
        overlay.setPickOnBounds(false);

        // Örnek kartları oluştur
        for (int i = 1; i <= 4; i++) {
            DownloadCard card = new DownloadCard("Modpack " + i, "Açıklama metni burada", i);
            card.setOnDownload(() -> {
                Node cardNode = card;                     // Kartın kendisi bir Node
                Node icon = card.getDownloadIcon();       // Sağ üstteki indirme simgesi
                overlay.startWarp(cardNode, icon, () -> {
                    cardNode.setVisible(true);            // Animasyon bitince kartı geri getir
                    card.setDownloaded(true);             // Kart durumunu güncelle
                });
            });
            cardPane.getChildren().add(card);
        }

        root.setCenter(cardPane);

        // Overlay'i en üste eklemek için StackPane kullan
        StackPane stackRoot = new StackPane();
        stackRoot.getChildren().addAll(root, overlay);

        var image = new Image(new FileInputStream("C:\\Users\\furka\\Desktop\\abc.png"));
        var view = new ImageView(image);
        view.setFitWidth(64);
        view.setFitHeight(64);
        view.setLayoutX(728);
        view.setLayoutY(728);
        view.setManaged(false);
        stackRoot.getChildren().add(view);

        // Overlay boyutunu sahneye bağla
        overlay.prefWidthProperty().bind(stackRoot.widthProperty());
        overlay.prefHeightProperty().bind(stackRoot.heightProperty());

        var stage = new Stage();

        Scene scene = new Scene(stackRoot, 900, 600);
        stage.setTitle("Minecraft Launcher - Warp Download");
        stage.setScene(scene);
        stage.show();
    }
}
