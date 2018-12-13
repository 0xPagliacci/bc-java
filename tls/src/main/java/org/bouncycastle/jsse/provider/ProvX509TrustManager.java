package org.bouncycastle.jsse.provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.Socket;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.Provider;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509TrustManager;

import org.bouncycastle.jsse.BCX509ExtendedTrustManager;

class ProvX509TrustManager
    extends BCX509ExtendedTrustManager
{
    static final Constructor<? extends X509TrustManager> exportX509TrustManagerConstructor;

    static
    {
        Constructor<? extends X509TrustManager> cons = null;
        try
        {
            Method[] methods = ReflectionUtil.getMethods("javax.net.ssl.X509ExtendedTrustManager");
            if (null != methods)
            {
                String className = "org.bouncycastle.jsse.provider.ExportX509TrustManager_7";

                cons = ReflectionUtil.getDeclaredConstructor(className,  ProvX509TrustManager.class);
            }
        }
        catch (Exception e)
        {
        }

        exportX509TrustManagerConstructor = cons;
    }

    private static X509TrustManager createExportX509TrustManager(ProvX509TrustManager x509TrustManager)
    {
        if (exportX509TrustManagerConstructor != null)
        {
            try
            {
                return exportX509TrustManagerConstructor.newInstance(x509TrustManager);
            }
            catch (Exception e)
            {
            }
        }

        return new ExportX509TrustManager_5(x509TrustManager);
    }

    private static Set<X509Certificate> getTrustedCerts(Set<TrustAnchor> trustAnchors)
    {
        Set<X509Certificate> result = new HashSet<X509Certificate>(trustAnchors.size());
        for (TrustAnchor trustAnchor : trustAnchors)
        {
            if (trustAnchor != null)
            {
                X509Certificate trustedCert = trustAnchor.getTrustedCert();
                if (trustedCert != null)
                {
                    result.add(trustedCert);
                }
            }
        }
        return result;
    }

    private final Provider pkixProvider;
    private final Set<X509Certificate> trustedCerts;
    private final PKIXParameters baseParameters;
    private final X509TrustManager exportX509TrustManager;

    ProvX509TrustManager(Provider pkixProvider, Set<TrustAnchor> trustAnchors)
        throws InvalidAlgorithmParameterException
    {
        this.pkixProvider = pkixProvider;
        this.trustedCerts = getTrustedCerts(trustAnchors);
        this.baseParameters = new PKIXBuilderParameters(trustAnchors, new X509CertSelector());
        this.baseParameters.setRevocationEnabled(false);
        this.exportX509TrustManager = createExportX509TrustManager(this);
    }

    ProvX509TrustManager(Provider pkixProvider, PKIXParameters baseParameters)
        throws InvalidAlgorithmParameterException
    {
        this.pkixProvider = pkixProvider;
        this.trustedCerts = getTrustedCerts(baseParameters.getTrustAnchors());
        if (baseParameters instanceof PKIXBuilderParameters)
        {
            this.baseParameters = baseParameters;
        }
        else
        {
            this.baseParameters = new PKIXBuilderParameters(baseParameters.getTrustAnchors(), baseParameters.getTargetCertConstraints());
            this.baseParameters.setCertStores(baseParameters.getCertStores());
            this.baseParameters.setRevocationEnabled(baseParameters.isRevocationEnabled());
            this.baseParameters.setCertPathCheckers(baseParameters.getCertPathCheckers());
            this.baseParameters.setDate(baseParameters.getDate());
            this.baseParameters.setAnyPolicyInhibited(baseParameters.isAnyPolicyInhibited());
            this.baseParameters.setPolicyMappingInhibited(baseParameters.isPolicyMappingInhibited());
            this.baseParameters.setExplicitPolicyRequired(baseParameters.isExplicitPolicyRequired());
        }
        this.exportX509TrustManager = createExportX509TrustManager(this);
    }

    X509TrustManager getExportX509TrustManager()
    {
        return exportX509TrustManager;
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType)
        throws CertificateException
    {
        // TODO: need to confirm cert and client identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType, Socket socket)
        throws CertificateException
    {
        // TODO: need to confirm cert and client identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public void checkClientTrusted(X509Certificate[] x509Certificates, String authType, SSLEngine engine)
        throws CertificateException
    {
        // TODO: need to confirm cert and client identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType)
        throws CertificateException
    {
        // TODO: need to confirm cert and server identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType, Socket socket)
        throws CertificateException
    {
        // TODO: need to confirm cert and server identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public void checkServerTrusted(X509Certificate[] x509Certificates, String authType, SSLEngine engine)
        throws CertificateException
    {
        // TODO: need to confirm cert and server identity match
        // TODO: need to make sure authType makes sense.
        validatePath(x509Certificates);
    }

    public X509Certificate[] getAcceptedIssuers()
    {
        return trustedCerts.toArray(new X509Certificate[trustedCerts.size()]);
    }

    protected void validatePath(X509Certificate[] x509Certificates)
        throws CertificateException
    {
        if (x509Certificates == null || x509Certificates.length < 1)
        {
            throw new IllegalArgumentException("'x509Certificates' must be a chain of at least one certificate");
        }

        X509Certificate eeCert = x509Certificates[0];
        if (trustedCerts.contains(eeCert))
        {
            return;
        }

        try
        {
            CertStore certStore = CertStore.getInstance("Collection",
                new CollectionCertStoreParameters(Arrays.asList(x509Certificates)), pkixProvider);

            CertPathBuilder pathBuilder = CertPathBuilder.getInstance("PKIX", pkixProvider);

            X509CertSelector constraints = (X509CertSelector)baseParameters.getTargetCertConstraints().clone();

            constraints.setCertificate(eeCert);

            PKIXBuilderParameters param = (PKIXBuilderParameters)baseParameters.clone();

            param.addCertStore(certStore);
            param.setTargetCertConstraints(constraints);

            @SuppressWarnings("unused")
            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult)pathBuilder.build(param);
        }
        catch (GeneralSecurityException e)
        {
            throw new CertificateException("unable to process certificates: " + e.getMessage(), e);
        }
    }
}
