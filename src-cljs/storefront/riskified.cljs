(ns storefront.riskified)

(def store-domain "mayvenn.com")

(defn insert-beacon [session-id]
  (let [protocol (if (= "https:" (.. js/document -location -protocol))
                   "https://"
                   "http://")
        src(str protocol "beacon.riskified.com?shop=" store-domain "&sid=" session-id)
        script-tag (.createElement js/document "script")
        first-script-tag (aget (.getElementsByTagName js/document "script") 0)]
    (set! (.-type script-tag) "text/javascript")
    (set! (.-async script-tag) "true")
    (set! (.-src script-tag) src)
    (.add (.-classList script-tag) "riskified-beacon")
    (.insertBefore (.-parentNode first-script-tag) script-tag first-script-tag)))

(defn remove-beacon []
  (when-let [beacon-tag (aget (.getElementsByClassName js/document "riskified-beacon") 0)]
    (.remove beacon-tag)))
