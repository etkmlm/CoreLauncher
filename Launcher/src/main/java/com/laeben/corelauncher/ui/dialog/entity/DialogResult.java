package com.laeben.corelauncher.ui.dialog.entity;

public sealed interface DialogResult<T> {
    record Completed<T>(T value) implements DialogResult<T> { }
    record Cancelled<T>() implements DialogResult<T> { }
    record Removed<T>() implements DialogResult<T> { }
}
