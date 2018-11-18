package org.bouncycastle.tls;

import java.io.IOException;
import java.util.Hashtable;

import org.bouncycastle.tls.crypto.TlsCrypto;
import org.bouncycastle.util.Arrays;

public class SRPTlsClient
    extends AbstractTlsClient
{
    private static final int[] DEFAULT_CIPHER_SUITES = new int[]
    {
        CipherSuite.TLS_SRP_SHA_RSA_WITH_AES_128_CBC_SHA
    };

    protected TlsSRPConfigVerifier srpConfigVerifier;

    protected byte[] identity;
    protected byte[] password;

    public SRPTlsClient(TlsCrypto crypto, byte[] identity, byte[] password)
    {
        this(crypto, new DefaultTlsKeyExchangeFactory(), new DefaultTlsSRPConfigVerifier(), identity, password);
    }

    public SRPTlsClient(TlsCrypto crypto, TlsKeyExchangeFactory keyExchangeFactory, TlsSRPConfigVerifier srpConfigVerifier,
        byte[] identity, byte[] password)
    {
        super(crypto, keyExchangeFactory);
        this.srpConfigVerifier = srpConfigVerifier;
        this.identity = Arrays.clone(identity);
        this.password = Arrays.clone(password);
    }

    protected int[] getSupportedCipherSuites()
    {
        return TlsUtils.getSupportedCipherSuites(context.getCrypto(), DEFAULT_CIPHER_SUITES);
    }

    protected boolean requireSRPServerExtension()
    {
        // No explicit guidance in RFC 5054; by default an (empty) extension from server is optional
        return false;
    }

    public Hashtable getClientExtensions()
        throws IOException
    {
        Hashtable clientExtensions = TlsExtensionsUtils.ensureExtensionsInitialised(super.getClientExtensions());
        TlsSRPUtils.addSRPExtension(clientExtensions, this.identity);
        return clientExtensions;
    }

    public void processServerExtensions(Hashtable serverExtensions)
        throws IOException
    {
        if (!TlsUtils.hasExpectedEmptyExtensionData(serverExtensions, TlsSRPUtils.EXT_SRP,
            AlertDescription.illegal_parameter))
        {
            if (requireSRPServerExtension())
            {
                throw new TlsFatalAlert(AlertDescription.illegal_parameter);
            }
        }

        super.processServerExtensions(serverExtensions);
    }

    public TlsKeyExchange getKeyExchange()
        throws IOException
    {
        int selectedCipherSuite = context.getSecurityParametersHandshake().getCipherSuite();
        int keyExchangeAlgorithm = TlsUtils.getKeyExchangeAlgorithm(selectedCipherSuite);

        switch (keyExchangeAlgorithm)
        {
        case KeyExchangeAlgorithm.SRP:
        case KeyExchangeAlgorithm.SRP_DSS:
        case KeyExchangeAlgorithm.SRP_RSA:
            return createSRPKeyExchange(keyExchangeAlgorithm);

        default:
            /*
             * Note: internal error here; the TlsProtocol implementation verifies that the
             * server-selected cipher suite was in the list of client-offered cipher suites, so if
             * we now can't produce an implementation, we shouldn't have offered it!
             */
            throw new TlsFatalAlert(AlertDescription.internal_error);
        }
    }

    public TlsAuthentication getAuthentication() throws IOException
    {
        /*
         * Note: This method is not called unless a server certificate is sent, which may be the
         * case e.g. for SRP_DSS or SRP_RSA key exchange.
         */
        throw new TlsFatalAlert(AlertDescription.internal_error);
    }

    protected TlsKeyExchange createSRPKeyExchange(int keyExchange) throws IOException
    {
        return keyExchangeFactory.createSRPKeyExchangeClient(keyExchange, srpConfigVerifier, identity, password);
    }
}
