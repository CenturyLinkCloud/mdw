/**
 * Copyright (c) 2014 CenturyLink, Inc. All Rights Reserved.
 */
package com.centurylink.mdw.common.translator;

import java.util.List;

import javax.xml.bind.JAXBException;

import org.osgi.framework.Bundle;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.centurylink.mdw.common.exception.TranslationException;
import com.centurylink.mdw.common.provider.PackageAwareProvider;
import com.centurylink.mdw.common.provider.Provider;
import com.centurylink.mdw.common.provider.ProviderRegistry;
import com.centurylink.mdw.common.provider.VariableTranslatorProvider;
import com.centurylink.mdw.model.value.variable.DocumentReference;
import com.centurylink.mdw.osgi.BundleSpec;
import com.centurylink.mdw.osgi.PackageAwareProviderLocator;
import com.centurylink.mdw.osgi.ProviderLocator;

public abstract class DocumentReferenceTranslator extends VariableTranslator {

    public final Object toObject(String str) throws TranslationException {
        int k = str.indexOf('@');
        if (k<0) return new DocumentReference(new Long(str.substring(9)), null);
        else return new DocumentReference(new Long(str.substring(9,k)), str.substring(k+1));
    }

    public final String toString(Object object) throws TranslationException {
        return ((DocumentReference)object).toString();
    }

    /**
     * toString converts DocumentReference to string,
     * whereas this method converts the real object to string
     * @param pObject
     * @return
     * @throws TranslationException
     */
    public abstract String realToString(Object pObject)
    throws TranslationException;

    /**
     * toObject converts String to DocumentReference
     * whereas this methods converts the string to real object
     * @param pStr
     * @return
     * @throws TranslationException
     */
    public abstract Object realToObject(String pStr)
    throws TranslationException;

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    protected Object realToObject(String str, boolean tryProviders) throws TranslationException {
        return realToObject(str);
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    protected String realToString(Object obj, boolean tryProviders) throws TranslationException {
        return realToString(obj);
    }

    /**
     * For OSGi use provider-based deserialization to avoid ClassNotFoundExceptions
     */
    protected Object providerDeserialize(final String string) throws TranslationException {
        return providerTranslate(string, new Translation() {
            public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception {
                return providedTranslator.realToObject(string, false);
            }
        });
    }

    protected String providerSerialize(final Object obj) throws TranslationException {
        return (String) providerTranslate(obj, new Translation() {
            public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception {
                return providedTranslator.realToString(obj, false);
            }
        });
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    public Document toDomDocument(Object obj, boolean tryProviders) throws TranslationException {
        if (this instanceof XmlDocumentTranslator)
            return ((XmlDocumentTranslator)this).toDomDocument(obj);
        else
            throw new UnsupportedOperationException("Translator: " + this.getClass().getName() + " does not implement" + XmlDocumentTranslator.class.getName());
    }

    /**
     * Default implementation ignores providers.  Override to try registry lookup.
     */
    public Object fromDomNode(Node domNode, boolean tryProviders) throws TranslationException {
        if (this instanceof XmlDocumentTranslator)
            return ((XmlDocumentTranslator)this).fromDomNode(domNode);
        else
            throw new UnsupportedOperationException("Translator: " + this.getClass().getName() + " does not implement" + XmlDocumentTranslator.class.getName());
    }

    protected Document providerToDomDoc(final Object obj) throws TranslationException {
        return (Document) providerTranslate(obj, new Translation() {
            public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception {
                return providedTranslator.toDomDocument(obj, false);
            }
        });
    }

    protected Object providerFromDomNode(final Node domNode) throws TranslationException {
        return providerTranslate(domNode, new Translation() {
            public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception {
                return providedTranslator.fromDomNode(domNode, false);
            }
        });
    }

    protected Object providerTranslate(Object in, Translation trans) {
        BundleSpec bundleSpec = getPackage() == null ? null : getPackage().getBundleSpec();
        if (bundleSpec == null)
            return doTranslate(in, trans, null);

        try {
            return doTranslate(in, trans, bundleSpec);
        }
        catch (TranslationException ex) {
            // fall back on any suitable translator
            return doTranslate(in, trans, null);
        }
    }

    private Object doTranslate(Object in, Translation trans, BundleSpec bundleSpec) {
        ProviderRegistry registry = ProviderRegistry.getInstance();
        Throwable notFound = new ClassNotFoundException("Unable to translate: " + in);

        List<PackageAwareProvider<VariableTranslator>> pkgAwareProviders = registry.getPackageAwareProviders(VariableTranslatorProvider.class);
        String latestMatchingPkgAwareVer = null;
        if (bundleSpec != null)
            latestMatchingPkgAwareVer = new PackageAwareProviderLocator<VariableTranslator>(pkgAwareProviders).getLatestMatchingBundleVersion(bundleSpec);

        for (PackageAwareProvider<VariableTranslator> provider : pkgAwareProviders) {
            Bundle bundle = provider.getBundleContext().getBundle();
            if (bundleSpec == null || (bundleSpec.meetsSpec(bundle) && bundle.getVersion().toString().equals(latestMatchingPkgAwareVer))) {
                DocumentReferenceTranslator providedTranslator = null;
                try {
                    providedTranslator = (DocumentReferenceTranslator)provider.getInstance(getPackage(), this.getClass().getName());
                }
                catch (ClassNotFoundException ex) {
                    // not located by this provider
                }
                catch (Exception ex) {
                    throw new TranslationException(ex.getMessage(), ex);
                }
                try {
                    if (providedTranslator != null) {
                        return trans.translate(in, providedTranslator);
                    }
                }
                catch (TranslationException ex) {
                    if (ex.getCause() instanceof ClassNotFoundException
                        || ex.getCause() instanceof NoClassDefFoundError
                        || ex.getCause() instanceof JAXBException) {
                        notFound = ex.getCause();
                        // try another provider
                    }
                    else {
                        throw ex;
                    }
                }
                catch (Exception ex) {
                    throw new TranslationException(ex.getMessage(), ex);
                }
            }
        }

        List<Provider<VariableTranslator>> providers = registry.getNonPackageAwareProviders(VariableTranslatorProvider.class);

        String latestMatchingVer = null;
        if (bundleSpec != null)
            latestMatchingVer = new ProviderLocator<VariableTranslator>(providers).getLatestMatchingBundleVersion(bundleSpec);

        for (Provider<VariableTranslator> provider : providers) {
            Bundle bundle = provider.getBundleContext().getBundle();
            if (bundleSpec == null || (bundleSpec.meetsSpec(bundle) && bundle.getVersion().toString().equals(latestMatchingVer))) {
                DocumentReferenceTranslator providedTranslator = null;
                try {
                    providedTranslator = (DocumentReferenceTranslator) provider.getInstance(this.getClass().getName());
                }
                catch (ClassNotFoundException ex) {
                    // not located by this provider
                }
                catch (Exception ex) {
                    throw new TranslationException(ex.getMessage(), ex);
                }
                try {
                    if (providedTranslator != null) {
                        return trans.translate(in, providedTranslator);
                    }
                }
                catch (TranslationException ex) {
                    if (ex.getCause() instanceof ClassNotFoundException
                        || ex.getCause() instanceof NoClassDefFoundError
                        || ex.getCause() instanceof JAXBException
                        || (ex.getCause() != null && ex.getCause().getCause() instanceof ClassNotFoundException)) {
                        notFound = ex.getCause();
                        // try another provider
                    }
                    else {
                        throw ex;
                    }
                }
                catch (Exception ex) {
                    throw new TranslationException(ex.getMessage(), ex);
                }
            }
        }

        throw new TranslationException(notFound.getMessage(), notFound);
    }

    interface Translation {
        public Object translate(Object in, DocumentReferenceTranslator providedTranslator) throws Exception;
    }
}
