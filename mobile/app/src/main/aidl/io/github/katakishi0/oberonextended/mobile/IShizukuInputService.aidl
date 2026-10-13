package io.github.katakishi0.oberonextended.mobile;

import io.github.katakishi0.oberonextended.mobile.IShizukuInputSink;

interface IShizukuInputService {
    void start(String deviceName, IShizukuInputSink sink);
    void stop();
    String whoami();
}
