package org.bouncycastle.tls;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.bouncycastle.tls.crypto.TlsCertificate;

/**
 * Parsing and encoding of a <i>Certificate</i> struct from RFC 4346.
 * <pre>
 * opaque ASN.1Cert&lt;2^24-1&gt;;
 *
 * struct {
 *     ASN.1Cert certificate_list&lt;0..2^24-1&gt;;
 * } Certificate;
 * </pre>
 *
 * @see org.bouncycastle.asn1.x509.Certificate
 */
public class Certificate
{
    public static final Certificate EMPTY_CHAIN = new Certificate(new TlsCertificate[0]);

    protected TlsCertificate[] certificateList;

    public Certificate(TlsCertificate[] certificateList)
    {
        if (certificateList == null)
        {
            throw new IllegalArgumentException("'certificateList' cannot be null");
        }

        this.certificateList = certificateList;
    }

    /**
     * @return an array of {@link org.bouncycastle.asn1.x509.Certificate} representing a certificate
     *         chain.
     */
    public TlsCertificate[] getCertificateList()
    {
        return cloneCertificateList();
    }

    public TlsCertificate getCertificateAt(int index)
    {
        return certificateList[index];
    }

    public int getLength()
    {
        return certificateList.length;
    }

    /**
     * @return <code>true</code> if this certificate chain contains no certificates, or
     *         <code>false</code> otherwise.
     */
    public boolean isEmpty()
    {
        return certificateList.length == 0;
    }

    /**
     * Encode this {@link Certificate} to an {@link OutputStream}.
     *
     * @param output the {@link OutputStream} to encode to.
     * @throws IOException
     * @deprecated
     */
    public void encode(OutputStream output)
        throws IOException
    {
        encode(null, output, null);
    }

    /**
     * Encode this {@link Certificate} to an {@link OutputStream}, and optionally calculate the
     * "end point hash" (per RFC 5929's tls-server-end-point binding).
     *
     * @param messageOutput the {@link OutputStream} to encode to.
     * @param endPointHashOutput the {@link OutputStream} to write the "end point hash" (or null).
     * @throws IOException
     */
    public void encode(TlsContext context, OutputStream messageOutput, OutputStream endPointHashOutput)
        throws IOException
    {
        Vector derEncodings = new Vector(this.certificateList.length);

        int totalLength = 0;
        for (int i = 0; i < this.certificateList.length; ++i)
        {
            TlsCertificate cert = certificateList[i];
            byte[] derEncoding = cert.getEncoded();

            if (i == 0 && endPointHashOutput != null)
            {
                calculateEndPointHash(context, cert, derEncoding, endPointHashOutput);
            }

            derEncodings.addElement(derEncoding);
            totalLength += derEncoding.length + 3;
        }

        TlsUtils.checkUint24(totalLength);
        TlsUtils.writeUint24(totalLength, messageOutput);

        for (int i = 0; i < derEncodings.size(); ++i)
        {
            byte[] derEncoding = (byte[])derEncodings.elementAt(i);
            TlsUtils.writeOpaque24(derEncoding, messageOutput);
        }
    }

    /**
     * Parse a {@link Certificate} from an {@link InputStream}.
     *
     * @param context
     *            the {@link TlsContext} of the current connection.
     * @param input
     *            the {@link InputStream} to parse from.
     * @return a {@link Certificate} object.
     * @throws IOException
     * @deprecated
     */
    public static Certificate parse(TlsContext context, InputStream input)
        throws IOException
    {
        return parse(context, input, null);
    }

    /**
     * Parse a {@link Certificate} from an {@link InputStream}.
     *
     * @param context
     *            the {@link TlsContext} of the current connection.
     * @param messageInput
     *            the {@link InputStream} to parse from.
     * @param endPointHashOutput the {@link OutputStream} to write the "end point hash" (or null).
     * @return a {@link Certificate} object.
     * @throws IOException
     */
    public static Certificate parse(TlsContext context, InputStream messageInput, OutputStream endPointHashOutput)
        throws IOException
    {
        int totalLength = TlsUtils.readUint24(messageInput);
        if (totalLength == 0)
        {
            return EMPTY_CHAIN;
        }

        byte[] certListData = TlsUtils.readFully(totalLength, messageInput);

        ByteArrayInputStream buf = new ByteArrayInputStream(certListData);

        Vector certificate_list = new Vector();
        while (buf.available() > 0)
        {
            byte[] derEncoding = TlsUtils.readOpaque24(buf, 1);
            TlsCertificate cert = context.getCrypto().createCertificate(derEncoding);
            
            if (certificate_list.isEmpty() && endPointHashOutput != null)
            {
                calculateEndPointHash(context, cert, derEncoding, endPointHashOutput);
            }

            certificate_list.addElement(cert);
        }

        TlsCertificate[] certificateList = new TlsCertificate[certificate_list.size()];
        for (int i = 0; i < certificate_list.size(); i++)
        {
            certificateList[i] = (TlsCertificate)certificate_list.elementAt(i);
        }
        return new Certificate(certificateList);
    }

    protected static void calculateEndPointHash(TlsContext context, TlsCertificate cert, byte[] encoding, OutputStream output)
        throws IOException
    {
        byte[] endPointHash = TlsUtils.calculateEndPointHash(context, cert.getSigAlgOID(), encoding);
        if (endPointHash != null && endPointHash.length > 0)
        {
            output.write(endPointHash);
        }
    }

    protected TlsCertificate[] cloneCertificateList()
    {
        TlsCertificate[] result = new TlsCertificate[certificateList.length];
        System.arraycopy(certificateList, 0, result, 0, result.length);
        return result;
    }
}
