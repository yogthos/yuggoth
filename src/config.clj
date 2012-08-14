(ns config)

(def blog-config
  ;;db config
  {:host "localhost"
   :schema "blogdb"
   :user "user"
   :pass "pass"
    ;;enable this to redirect login to HTTPS 
    ;;make sure that the container has an HTTPS listener setup 
    ;;if you're listening on a non standard SSL port (not 443), you will have to change the port above
    ;;I haven't found a way to get the port from the container   
   :ssl false
   :ssl-port 443})


