(ns app.jira.auth
  (:require [app.config :refer [config]]
            [oauth.client :as oauth])
  (:import java.util.Base64
           org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
           org.bouncycastle.openssl.PEMParser
           com.google.api.client.auth.oauth.OAuthRsaSigner
           (org.bouncycastle.openssl.jcajce JcaPEMKeyConverter
                                            JceOpenSSLPKCS8DecryptorProviderBuilder)))

(def private-key (-> config :jira :private-key))
(def pass-phrase (-> config :jira :pass-phrase))
(def consumer-key (-> config :jira :consumer-key))
(def access-token-url (-> config :jira :access-token-url))
(def temporary-token-url (-> config :jira :temporary-token-url))
(def auth-url (-> config :jira :auth-url))

(defn decode [to-decode]
  (.decode (Base64/getDecoder) to-decode))

(def signer
  (do
    (java.security.Security/addProvider
      (org.bouncycastle.jce.provider.BouncyCastleProvider.))
    (let [private-key (decode private-key)
          pem-parser  (-> private-key
                          java.io.ByteArrayInputStream.
                          java.io.InputStreamReader.
                          PEMParser.)
          enc-PK-info (->> (.readObject pem-parser)
                           (cast PKCS8EncryptedPrivateKeyInfo))
          dec-prov    (-> (JceOpenSSLPKCS8DecryptorProviderBuilder.)
                          (.setProvider "BC")
                          (.build (.toCharArray pass-phrase)))
          pk-info     (.decryptPrivateKeyInfo enc-PK-info dec-prov)
          key         (-> (JcaPEMKeyConverter.)
                          (.setProvider "BC")
                          (.getPrivateKey pk-info))
          signer      (OAuthRsaSigner.)]
      (set!  (.-privateKey signer) key)
      signer)))

(defmethod oauth.signature/sign :rsa-sha1
  [_ ^String base-string & [_]]
  (let [s signer]
    (.computeSignature s base-string)))

(def consumer (oauth/make-consumer consumer-key
                                   private-key
                                   temporary-token-url
                                   access-token-url
                                   auth-url
                                   :rsa-sha1))

(defn request-token []
  (oauth/request-token consumer  "http://localhost:8666/login"))

(defn get-approval-url [request-token]
  (oauth/user-approval-uri consumer
                           (:oauth_token request-token)))

(defn access-token-response [verifier request-token]
  (oauth/access-token consumer
                      request-token
                      verifier))
