A. Create a Server Identity Keystore:
    0. If not already trusted, import ctlcertnew.cer into your local cacerts trust keystore:
    
    keytool -v -import -trustcacerts -alias centurylink -file ctlcertnew.cer -keystore C:\jdk1.6.0_39\jre\lib\security\cacerts
    Enter keystore password:  changeit

    1. Generate an identity keystore for the server:
       (Note: for host certificates, use the fully-qualified domain name for the "first and last name").

    keytool -genkey -keyalg RSA -sigalg SHA1withRSA -alias myself -keypass guest1 -keystore mdw.jks -storepass guest1
    
    What is your first and last name?
      [Unknown]:  MDW Framework
    What is the name of your organizational unit?
      [Unknown]:  CenturyLink ITS
    What is the name of your organization?
      [Unknown]:  CenturyLink
    What is the name of your City or Locality?.
      [Unknown]:  Denver
    What is the name of your State or Province?
      [Unknown]:  CO
    What is the two-letter country code for this unit?
      [Unknown]:  US
    Is CN=MDW Framework, OU=Centurylink ITS, O=CenturyLink, L=Denver, ST=CO, C=US correct?
      [no]:  yes

    2. Dump the generated keystore to a text file:
    
    keytool -certreq -alias myself -keypass guest1 -keystore mdw.jks -storepass guest1 -file mdw_cert.txt
    
    3. Go to https://dotnet1ca.uswc.uswest.com/CertSrv and elect to "Request a certificate".
       Click on "Advanced certificate request."
       Click on "Submit a certificate request by using a base-64-encoded CMC..."
       Paste exported certificate request.
       Submit the request for Base 64 Encoded and note the Request ID.
       Fill out the post-submission enrollment form for Test Certificates.
       Await the e-mail or check the website for the certificate.
       Save as mdw.p7b
       
    NOTE: THIS ONLY WORKS ONCE.  FOR RE-IMPORT A NEW JKS MUST BE CREATED AND A NEW CERTIFICATE MUST BE REQUESTED.

    4. Import the generated cert file into your identity keystore:
    
    keytool -import -v -file mdw.p7b -trustcacerts -alias myself -keystore mdw.jks -keypass guest1 -storepass guest1
    (The generated jks file is your Server Identity Keystore).
    
    5. Download the CTL CA cert from https://dotnet1ca.uswc.uswest.com/CertSrv and import into your identity store:

    keytool -import -v -file certnew.cer -trustcacerts -alias qwestca -keystore mdw.jks

    6. Print the certificate contents to verify:
    
    keytool -export -v -file mdw.crt -trustcacerts -alias myself -keystore mdw.jks -keypass guest1 -storepass guest1
    keytool -printcert -file mdw.crt > mdw.out
    keytool -v -list -keystore mdw.jks -keypass guest1 -storepass guest1 > mdw.out2
    
    
To export from a JKS and create an Apache certificate for SSL:

    keytool -importkeystore -srckeystore lxdenvmtc144.jks -srcstoretype JKS -destkeystore lxdenvmtc144.p12 -deststoretype PKCS12
    (ignore warning about "TrustedCertEntry not supported")
    
    ->export certificate (SSLCertificateFile in ssl.conf)
    openssl pkcs12 -in lxdenvmtc144.p12 -clcerts -nokeys -out cacert.pem
    
    ->export private key (SSLCertificateKeyFile in ssl.conf)
    openssl pkcs12 -in lxdenvmtc144.p12 -nocerts -nodes -out cakey.pem    

lxdenvmtc144 cert details: alias=lxdenvmtc144 password=d3vqint!
