(ns app.routes)

(defmulti multi-handler
  :handler)

(def routes
  ["/" [["" :home]
        ["auth-link" {:get :auth-link}]
        ["oauth" {:post :oauth}]
        ["user-name" {:post :user-name}]
        ["ping" {:post :save-ping}]
        ["active-task" {:post   :save-track-logs
                        :delete :delete-track-logs}]
        ["track-logs" {:post :get-track-logs}]
        ["project" {:post :project}]
        ["submit" {:post :submit}]
        ["tasks" {:post :tasks}]
        ["by-project-id" {:post :by-project-id}]
        ]])
