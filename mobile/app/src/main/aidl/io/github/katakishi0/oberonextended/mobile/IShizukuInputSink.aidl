package io.github.katakishi0.oberonextended.mobile;

oneway interface IShizukuInputSink {
    void onStatus(String status);
    void onRanges(String ranges);
    void onEvents(String events);
}
