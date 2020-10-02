(ns app.jira.auth
  (:require [app.config :refer [config]]
            [clj-http.client :as client]
            [oauth.client :as oauth]
            [app.repository.redis :as redis])
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

(comment
  ;; (reset! verifier "asdf123j")
  (client/get "http://localhost:2990/jira/rest/timetracker/api/3/user"
              {:query-params {:os_username "#admin" :os_password "admin"}
               :debug        true})

  @verifier
  verifier

  (request-token)

  (oauth/access-token consumer
                      {:oauth_token
                       "U2DsU5iUMF6XSN6OmVADV0iBcGvMPfWe"}
                      "2bywrB")

  (add-watch verifier :watcher
             (fn [key atom old-state new-state]
               (prn "-- Atom Changed --")
               (prn "key" key)
               (prn "atom" atom)
               (prn "old-state" old-state)
               (prn "new-state" new-state)))

  (add-watch verifier :watcher
             (fn [_ _ _ new-state]
               (print new-state)
               (let [access-token-response (oauth/access-token consumer
                                                               request-token
                                                               new-state)
                     credentials           (oauth/credentials consumer
                                                              (:oauth_token access-token-response)
                                                              (:oauth_token_secret access-token-response)
                                                              :GET
                                                              "http://192.168.1.103:2990/jira/rest/timetracker/api/3/user")]
                 (print (client/get "http://192.168.1.103:2990/jira/rest/timetracker/api/3/user"
                                    {:query-params credentials
                                     :debug        true})))))

  (def request-token (oauth/request-token consumer "http://192.168.1.103:3000/oauth"))

  (oauth/user-approval-uri consumer
                           (:oauth_token request-token))

  (def access-token-response (oauth/access-token consumer
                                                 request-token
                                                 @verifier))
  (def conss 
    {:token "YxUmYRnEptfDgVoDMZF334YevwKSvxA6", :secret "s4xT7GA0J5rAqRLddH6ge2NVv4sJxIRL"}
    )

  (def credentials (oauth/credentials consumer
                                      (:token conss)
                                      (:secret conss)
                                      :POST
                                      "http://localhost:2990/jira/rest/timetracker/api/3/tasks"))

  (def query {:key "test"})

  (client/post "http://localhost:2990/jira/rest/timetracker/api/3/tasks"
               {:query-params credentials
                :form-params  query
                :debug        true})
  )
