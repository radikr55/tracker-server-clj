(ns app.controllers.ping
  (:require   [app.routes :refer [multi-handler]]
              [app.responses :as responses]
              [app.repository.track-log-repo :as track-repo]
              [app.repository.active-task-repo :as active-repo]
              [app.service.track-log-service :as service]
              [app.service.submit-service :as subservice]
              [app.jira.api :as api]
              [clj-time.core :as t]
              [app.utils :as utils]
              [clojure.pprint :refer [pprint]]))

(defmethod multi-handler :get-track-logs
  [{:keys [body user-id]}]
  (try
    (let [offset       (:offset body)
          start        (utils/formatlocal (:start body) offset)
          end          (utils/formatlocal (:end body) offset)
          active-tasks (->> (active-repo/load-active-tasks user-id)
                            (map :task))
          track-logs   (track-repo/load-track-logs start end user-id)
          task-codes   (->> track-logs
                            (map :task)
                            (concat active-tasks)
                            (into #{})
                            (filter #(seq %))
                            (map #(assoc {} :issueCode %)))
          issue-list   (->> (assoc  body
                                    :query {:list      task-codes
                                            :endDate   end
                                            :startDate start})
                            (assoc {:endpoint :issue
                                    :json     true
                                    :method   :POST} :body)
                            (api/load-resource))]
      (responses/ok (assoc {} :data track-logs :desc issue-list)))
    (catch Exception e
      (responses/error (ex-data e) e))))

(defmethod multi-handler :save-ping
  [{:keys [body user-id]}]
  (pprint body)
  (try
    (let [offset         (:offset body)
          map->track-log (fn [item]
                           (let [start        (utils/formatlocal (:start item) offset)
                                 end          (utils/formatlocal (:end item) offset)
                                 status       (:status item)
                                 task         (:task item)
                                 inactive-log (= "inactive" status)]
                             {:start        start
                              :end          end
                              :inactive_log inactive-log
                              :uuid         (.toString (java.util.UUID/randomUUID))
                              :status       status
                              :task         task
                              :log_length   (utils/get-interval start end)
                              :user_id      user-id}))
          track-logs     (->> body
                              :data
                              (map map->track-log)
                              (filter #(not (t/equal? (:start %) (:end %)))))]
      (doseq [log track-logs]
        (service/save-track-logs user-id log)))
    (responses/ok {:test 123})
    (catch Exception e
      (responses/error (ex-data e) e))))

(comment

  ;; {"name" : "r.shylo", "token" : "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP", "secret" : "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3", "data" : [{"start" : "2020-11-01T20:04:00.000Z", "end" : "2020-11-01T20:06:00.000Z", "status" : "active", "task" : "WELKIN-76", "inactive" :false},{"start" : "2020-11-01T20:06:00.000Z", "end" : "2020-11-01T20:32:00.000Z", "status" : "inactive", "task" : "WELKIN-76", "inactive" :true},{"start" : "2020-11-01T20:32:00.000Z", "end" : "2020-11-01T20:33:00.000Z", "status" : "active", "task" : "WELKIN-76", "inactive" :false}], "offset" :-120}

  ;; #js {:method "post", :baseURL "http://localhost:3000/", :url "/ping", :params nil, :data #js {:name "r.shylo", :token "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP", :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3", :data #js [#js {:start "2020-11-01T20:04:00.000Z", :end "2020-11-01T20:06:00.000Z", :status "active", :task "WELKIN-76", :inactive false} #js {:start "2020-11-01T20:06:00.000Z", :end "2020-11-01T20:32:00.000Z", :status "inactive", :task "WELKIN-76", :inactive true} #js {:start "2020-11-01T20:32:00.000Z", :end "2020-11-01T20:33:00.000Z", :status "active", :task "WELKIN-76", :inactive false}], :offset -120}}

  ;; {:status 200, :statusText OK, :headers {:connection close, :date Sun, 01 Nov 2020 18:33:34 GMT, :content-type application/json;charset=utf-8, :content-length 12, :server Jetty(9.4.28.v20200408)}, :config {:maxBodyLength -1, :adapter #object[httpAdapter], :baseURL http://localhost:3000/, :xsrfHeaderName X-XSRF-TOKEN, :transformResponse [#object[transformResponse]], :method post, :validateStatus #object[validateStatus], :params nil, :headers {:Accept application/json, text/plain, */*, :Content-Type application/json;charset=utf-8, :User-Agent axios/0.20.0, :Content-Length 501}, :xsrfCookieName XSRF-TOKEN, :url /ping, :timeout 0, :maxContentLength -1, :transformRequest [#object[transformRequest]], :data {"name":"r.shylo","token":"o4zAKoL9twxHwUIN0zMTc04P38fAC8tP","secret":"aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3","data":[{"start":"2020-11-01T20:04:00.000Z","end":"2020-11-01T20:06:00.000Z","status":"active","task":"WELKIN-76","inactive":false},{"start":"2020-11-01T20:06:00.000Z","end":"2020-11-01T20:32:00.000Z","status":"inactive","task":"WELKIN-76","inactive":true},{"start":"2020-11-01T20:32:00.000Z","end":"2020-11-01T20:33:00.000Z","status":"active","task":"WELKIN-76","inactive":false}],"offset":-120}}, :request #object[ClientRequest [object Object]], :data {:test 123}}
  ;; #object[Error Error: Can't pop empty vector]
  ;; #js {:method "post", :baseURL "http://localhost:3000/", :url "/ping", :params nil, :data #js {:name "r.shylo", :token "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP", :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3", :data #js [#js {:start "2020-11-01T20:33:00.000Z", :end "2020-11-01T20:35:00.000Z", :status "active", :task "WELKIN-76", :inactive false} #js {:start "2020-11-01T20:35:00.000Z", :end "2020-11-01T20:36:00.000Z", :status "active", :task "WELKIN-76", :inactive false}], :offset -120}}

  ;; (multi-handler {:handler :get-track-logs
  ;;                 :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
  ;;                           :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
  ;;                           :offset 0
  ;;                           :start  "2020-10-26T19:05:00.000Z",
  ;;                           :end    "2020-10-26T20:00:00.000Z",
  ;;                           }})

  ;; (active-repo/load-active-tasks 166)
  (multi-handler {:handler :save-ping
                  :user-id 166
                  :body    {:name   "r.shylo",
                            :token  "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP",
                            :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3",
                            :data
                            [{:start    "2020-11-07T19:03:00.000Z",
                              :end      "2020-11-07T19:05:00.000Z",
                              :status   "active",
                              :task     "WELKIN-57",
                              :inactive false}
                             {:start    "2020-11-07T22:09:00.000Z",
                              :end      "2020-11-07T22:09:00.000Z",
                              :status   "active",
                              :task     "WELKIN-57",
                              :inactive false}
                             {:start    "2020-11-07T22:09:00.000Z",
                              :end      "2020-11-07T22:18:00.000Z",
                              :status   "inactive",
                              :task     "",
                              :inactive true}
                             {:start    "2020-11-07T22:18:00.000Z",
                              :end      "2020-11-07T22:19:00.000Z",
                              :status   "active",
                              :task     "WELKIN-57",
                              :inactive false}],
                            :offset -120}})

  (multi-handler {:handler :save-ping
                  :user-id 166
                  :body    {:name   "r.shylo",
                            :token  "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP",
                            :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3",
                            :data
                            [{:start    "2020-11-01T20:57:00.000Z",
                              :end      "2020-11-01T20:58:00.000Z",
                              :status   "active",
                              :task     "WELKIN-76",
                              :inactive false}],
                            :offset -120}})

  (multi-handler {:handler :save-ping
                  :user-id 166
                  :body    {:name   "r.shylo",
                            :token  "o4zAKoL9twxHwUIN0zMTc04P38fAC8tP",
                            :secret "aS3BBfU1TuMIZRdp6UGmcQVkOZdgweg3",
                            :data
                            [{:start    "2020-11-01T20:56:00.000Z",
                              :end      "2020-11-01T20:56:00.000Z",
                              :status   "active",
                              :task     "WELKIN-76",
                              :inactive false}
                             {:start    "2020-11-01T20:57:00.000Z",
                              :end      "2020-11-01T20:58:00.000Z",
                              :status   "active",
                              :task     "WELKIN-76",
                              :inactive false}
                             {:start    "2020-11-01T20:59:00.000Z",
                              :end      "2020-11-01T20:59:00.000Z",
                              :status   "active",
                              :task     "WELKIN-76",
                              :inactive false}],
                            :offset -120}})

  (multi-handler {:handler :save-ping
                  :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                            :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                            :offset -120
                            :data   [{:start    "2020-10-17T10:57:00.000Z",
                                      :end      "2020-10-17T11:57:00.000Z",
                                      :status   "active",
                                      :task     "WELKIN-76",
                                      :inaclive true,
                                      :user_id  166}]}})

  (multi-handler {:handler :save-ping
                  :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                            :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                            :offset -120
                            :data   [{:start        "2020-10-30T20:15:00.000Z",
                                      :end          "2020-10-30T20:42:00.000Z",
                                      :inactive_log false,
                                      :uuid         "2a6b04c6-6f23-496e-bfe9-2ec20c6a84da",
                                      :task         "WELKIN-76",
                                      :status       "active",
                                      :log_length   27,
                                      :user_id      166}
                                     {:start        "2020-10-30T20:43:00.000Z",
                                      :end          "2020-10-30T20:43:00.000Z",
                                      :inactive_log false,
                                      :status       "inactive",
                                      :uuid         "cc7e7daf-d977-42c7-9b01-d78bad05b28f",
                                      :task         "WELKIN-76",
                                      :log_length   0,
                                      :user_id      166}
                                     {:start        "2020-10-30T20:44:00.000Z",
                                      :end          "2020-10-30T20:49:00.000Z",
                                      :inactive_log false,
                                      :status       "active",
                                      :uuid         "1c0ec0ec-8320-43fe-b88f-245f5d80eab5",
                                      :task         "WELKIN-76",
                                      :log_length   5,
                                      :user_id      166}]}})

  ;; (multi-handler {:handler :save-ping
  ;;                 :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
  ;;                           :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
  ;;                           :offset -120
  ;;                           :data   [{:start    "2020-10-17T10:57:00.000Z",
  ;;                                     :end      "2020-10-17T11:57:00.000Z",
  ;;                                     :status   "inactive",
  ;;                                     :task     "WELKIN-76",
  ;;                                     :inaclive true,
  ;;                                     :user_id  166}]}})

  (multi-handler {:handler :save-ping
                  :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                            :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                            :offset -120
                            :data   [{:start    "2020-10-17T10:55:00.000Z",
                                      :end      "2020-10-17T12:57:00.000Z",
                                      :status   "inactive",
                                      :task     "WELKIN-76",
                                      :inaclive true,
                                      :user_id  166}]}})

  (do (multi-handler {:handler :save-ping
                      :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                                :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                                :offset -120
                                :data   [{:start    "2020-10-17T10:50:00.000Z",
                                          :end      "2020-10-17T10:57:00.000Z",
                                          :status   "active",
                                          :task     "WELKIN-76",
                                          :inaclive false,
                                          :user_id  166}]}})
      (multi-handler {:handler :save-ping
                      :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                                :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                                :offset -120
                                :data   [{:start    "2020-10-17T11:57:00.000Z",
                                          :end      "2020-10-17T12:57:00.000Z",
                                          :status   "inactive",
                                          :task     "WELKIN-76",
                                          :inaclive true,
                                          :user_id  166}]}})
      (multi-handler {:handler :save-ping
                      :body    {:token  "JvQH2GElcHLqSsGvAYAFipdNwGd92XpR"
                                :secret "aL9MBlTHh87AYC8iIpPdrUiGd8f5MrjG"
                                :offset -120
                                :data   [{:start    "2020-10-17T11:00:00.000Z",
                                          :end      "2020-10-17T11:57:00.000Z",
                                          :status   "active",
                                          :task     "WELKIN-76",
                                          :inaclive false,
                                          :user_id  166}]}}))

  (str (name :tasd) "123"))


(defmethod multi-handler :submit
  [request]
  (try
    (subservice/submit-time (:body request))
    (responses/ok {})
    (catch Exception e
      (responses/error (ex-data e) e))))
