(ns app.routes)

(defmulti multi-handler
  :handler)

(def routes
  ["/" [["" :home]
        ["login" {:post :login}]
        ["register" :register]
        ["oauth" {:post :oauth}]
        ["issue" {:get :issue}]
        ["project" {:post :project}]
        ["tasks" {:post :tasks}]
        ]])
