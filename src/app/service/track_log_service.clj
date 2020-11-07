(ns app.service.track-log-service
  (:require [clj-time.coerce :as c]
            [clj-time.core :as t]
            [app.utils :as utils]
            [app.repository.track-log-repo :as track-repo]
            [clojure.pprint :refer [pprint]]))

(defn set-top [coll & x]
  (apply conj (pop coll) x))

(defn reset-date
  ([item start end & args]
   (apply assoc (reset-date item start end) args))
  ([item start end]
   (assoc item
          :start start
          :end end
          ;; :log_length (utils/get-interval start end)
          )))

(defmulti ->update :type)

(defmethod ->update :end  [{:keys [task?
                                   update?
                                   override?
                                   new-start
                                   new-end
                                   ex-id
                                   ex-start
                                   ex-end
                                   ex-inactive?
                                   new
                                   track-logs]}]
  (let [new-left        (reset-date new new-start ex-end
                                    :inactive_log ex-inactive?)
        new-right       (reset-date new ex-end new-end)
        update-ex-end   #(track-repo/update-field
                           {:columns {:end_date   (utils/to-sql-date new-end)
                                      :log_length (utils/get-interval ex-start new-end)}
                            :id      ex-id})
        update-ex-start #(track-repo/update-field
                           {:columns {:end_date   (utils/to-sql-date new-start)
                                      :log_length (utils/get-interval ex-start new-start)}
                            :id      ex-id})
        delete-ex       #(track-repo/delete-log %)]
    (cond
      (and task? update?) (do (update-ex-end)
                              (when (:id new) (delete-ex (:id new)))
                              (set-top track-logs (reset-date new ex-start new-end
                                                              :id ex-id)))
      override?           (do (update-ex-start)
                              (set-top track-logs new))
      (not task?)         (do (update-ex-start)
                              (set-top track-logs new-left new-right))
      :else               (set-top track-logs (reset-date new ex-end new-end)))))

(defmethod ->update :start  [{:keys [task?
                                     update?
                                     override?
                                     new-start
                                     new-end
                                     ex-id
                                     ex-start
                                     ex-end
                                     ex-inactive?
                                     new
                                     track-logs]}]
  (let [new-left        (reset-date new new-start ex-start)
        new-right       (reset-date new ex-start new-end
                                    :inactive_log ex-inactive?)
        update-ex-end   #(track-repo/update-field
                           {:columns {:start_date (utils/to-sql-date new-start)
                                      :log_length (utils/get-interval new-start ex-end)}
                            :id      ex-id})
        update-ex-start #(track-repo/update-field
                           {:columns {:start_date (utils/to-sql-date new-end)
                                      :log_length (utils/get-interval new-end ex-end)}
                            :id      ex-id})
        delete-ex       #(track-repo/delete-log %)]
    (cond
      (and task? update?) (do (update-ex-end)
                              (when (:id new) (delete-ex (:id new)))
                              (set-top track-logs (reset-date new new-start ex-start
                                                              :id ex-id)))
      override?           (do (update-ex-start)
                              (set-top track-logs new))
      (not task?)         (do (update-ex-start)
                              (set-top track-logs new-left new-right))
      :else               (set-top track-logs (reset-date new new-start ex-start)))))

(defmethod ->update :middle  [{:keys [task?
                                      override?
                                      new-start
                                      new-end
                                      ex
                                      ex-id
                                      ex-start
                                      ex-end
                                      ex-inactive?
                                      new
                                      track-logs]}]
  (let [create-ex-left  #(track-repo/update-field
                           {:columns {:end_date   (utils/to-sql-date new-start)
                                      :log_length (utils/get-interval ex-start new-start)}
                            :id      ex-id})
        create-ex-right #(track-repo/save-exist
                           (merge ex  {:start_date (utils/to-sql-date new-end)
                                       :log_length (utils/get-interval new-end ex-end)}))]
    (cond

      (or (not task?) override?) (do (create-ex-left)
                                     (create-ex-right)
                                     (set-top track-logs (reset-date new new-start new-end
                                                                     :inactive_log ex-inactive?)))
      :else                      [])))

(defmethod ->update :all  [{:keys [update?
                                   task?
                                   new-task
                                   new-start
                                   new-end
                                   ex-id
                                   ex-start
                                   ex-end
                                   ex-inactive?
                                   override?
                                   new
                                   p-task
                                   p-inactive?
                                   p-id
                                   p-start
                                   track-logs]}]
  (let [new-left      (reset-date new new-start ex-start)
        new-right     (reset-date new ex-end new-end :id nil)
        new-id        (:id new)
        update-task   #(track-repo/update-field
                         {:columns {:task       new-task
                                    :log_length (utils/get-interval ex-start ex-end)}
                          :id      ex-id})
        update-p-end  #(track-repo/update-field
                         {:columns {:end_date   (utils/to-sql-date ex-end)
                                    :log_length (utils/get-interval p-start ex-end)}
                          :id      p-id})
        update-ex-end #(track-repo/update-field
                         {:columns {:end_date   (utils/to-sql-date ex-start)
                                    :log_length (utils/get-interval ex-start ex-end)}
                          :id      new-id})]
    (cond
      (and (= p-task new-task)
           (= ex-inactive? p-inactive?)) (do (track-repo/delete-log ex-id)
                                             (update-p-end)
                                             (set-top track-logs (reset-date new ex-end new-end)))
      (or update? override?)             (do (track-repo/delete-log ex-id)
                                             (set-top track-logs new))
      (not task?)                        (do (update-task)
                                             (if (some? new-id)
                                               (do (update-ex-end)
                                                   (set-top track-logs new-right))
                                               (set-top track-logs new-left new-right)))
      (not update?)                      (if  (some? new-id)
                                           (do (update-ex-end)
                                               (set-top track-logs new-right))
                                           (set-top track-logs new-left new-right)))))

(defn check->update
  "task? - task equal
  update? - status equal
  override? - status !equal inactive->active"
  [track-logs exist previously]
  (let [track-log     (last track-logs)
        new-start     (:start track-log)
        new-end       (:end track-log)
        new-inactive? (:inactive_log track-log)
        new-task      (:task track-log)
        p-task        (:task previously)
        p-inactive?   (:inactive_log previously)
        p-id          (:id previously)
        p-start       (c/from-date (:start_date previously))
        ex-start      (c/from-date (:start_date exist))
        ex-end        (c/from-date (:end_date exist))
        ex-task       (:task exist)
        ex-id         (:id exist)
        ex-inactive?  (:inactive_log exist)
        task?         (= new-task ex-task)
        update?       (= new-inactive? ex-inactive?)
        override?     (and ex-inactive? (not new-inactive?))
        type          (cond
                        ;; Es<Ns && Ee>Ne
                        (and (t/before? ex-start new-start)
                             (t/after? ex-end new-end))       :middle
                        ;; Es>Ns && Ee<Ne
                        (and (or (t/equal? ex-start new-start)
                                 (t/after? ex-start new-start))
                             (or (t/equal? ex-end new-end)
                                 (t/before? ex-end new-end))) :all
                        ;; Es<Ns && Ee<=Ne
                        (and (t/before? ex-start new-start)
                             (or (t/before? ex-end new-end)
                                 (t/equal? ex-end new-end)))  :end
                        ;; Es>=Ns && Ee>Ne
                        (and (or (t/after? ex-start new-start)
                                 (t/equal? ex-start new-start))
                             (t/after? ex-end new-end))       :start)]
    (->update {:type          type
               :new-start     new-start
               :new-end       new-end
               :new-task      new-task
               :new-inactive? new-inactive?
               :new           track-log
               :task?         task?
               :update?       update?
               :override?     override?
               :p-task        p-task
               :p-inactive?   p-inactive?
               :p-id          p-id
               :p-start       p-start
               :ex            exist
               :ex-id         ex-id
               :ex-start      ex-start
               :ex-inactive?  ex-inactive?
               :ex-end        ex-end
               :ex-task       ex-task
               :track-logs    track-logs})))

(defn track-log-update [track-log intersections]
  (loop [origin     intersections
         previously nil
         result     [track-log]]
    (cond (first origin) (let [exist (first origin)
                               res   (check->update result exist previously)]
                           (when (last res)
                             (recur (next origin) exist res)))
          :else          result)))

(defn save-track-logs
  [user-id track-log]
  (let [new-start         (t/minus- (:start track-log) (t/minutes 1))
        new-end           (:end track-log)
        intersection-logs (track-repo/load-intersection new-start new-end user-id)]
    (cond
      (empty? intersection-logs) (track-repo/save track-log)
      :else                      (doseq  [item (track-log-update track-log intersection-logs)]
                                   (let [start      (:start item)
                                         end        (:end item)
                                         length     (utils/get-interval start end)
                                         id         (:id item)
                                         log-lenght (:log_length item)]
                                     (cond
                                       (not id)                  (track-repo/save (assoc item :log_length length))
                                       (and id (= 0 log-lenght)) (track-repo/delete-log id)))))))


