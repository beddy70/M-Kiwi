/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package org.somanybits.minitel;

import javax.net.ssl.*;
import java.net.*;
import java.io.*;

abstract class DelegatingSSLSocketFactory extends SSLSocketFactory {

    private final SSLSocketFactory delegate;

    protected DelegatingSSLSocketFactory(SSLSocketFactory delegate) {
        this.delegate = delegate;
    }

    protected SSLSocket configure(SSLSocket socket) {
        return socket;
    }

    @Override
    public String[] getDefaultCipherSuites() {
        return delegate.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {
        return delegate.getSupportedCipherSuites();
    }

    @Override
    public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
        return configure((SSLSocket) delegate.createSocket(s, host, port, autoClose));
    }

    @Override
    public Socket createSocket(String host, int port) throws IOException {
        return configure((SSLSocket) delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(String host, int port, InetAddress local, int localPort) throws IOException {
        return configure((SSLSocket) delegate.createSocket(host, port, local, localPort));
    }

    @Override
    public Socket createSocket(InetAddress host, int port) throws IOException {
        return configure((SSLSocket) delegate.createSocket(host, port));
    }

    @Override
    public Socket createSocket(InetAddress addr, int port, InetAddress local, int localPort) throws IOException {
        return configure((SSLSocket) delegate.createSocket(addr, port, local, localPort));
    }
}
