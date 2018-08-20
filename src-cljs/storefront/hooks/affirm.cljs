(ns storefront.hooks.affirm
  (:require [storefront.config :as config]
            [storefront.browser.tags :as tags]
            [storefront.platform.messages :refer [handle-message]]
            [storefront.events :as events]
            [clojure.set :as set]))

(defn affirm-loaded? []
  (.hasOwnProperty js/window "affirm"))

(defn insert []
  (let [affirm-anon-fn "(function(l,g,m,e,a,f,b){var d,c=l[m]||{},h=document.createElement(f),n=document.getElementsByTagName(f)[0],k=function(a,b,c){return function(){a[b]._.push([c,arguments])}};c[e]=k(c,e,\"set\");d=c[e];c[a]={};c[a]._=[];d._=[];c[a][b]=k(c,a,b);a=0;for(b=\"set add save post open empty reset on off trigger ready setProduct\".split(\" \");a<b.length;a++)d[b[a]]=k(c,e,b[a]);a=0;for(b=[\"get\",\"token\",\"url\",\"items\"];a<b.length;a++)d[b[a]]=function(){};h.async=!0;h.src=g[f];n.parentNode.insertBefore(h,n);delete g[f];d(g);l[m]=c})(window,_affirm_config,\"affirm\",\"checkout\",\"ui\",\"script\",\"ready\");"
        config (-> {:public_api_key config/affirm-public-api-key
                    :script         config/affirm-script-uri}
                   clj->js
                   js/JSON.stringify)
        assignment-var (str "_affirm_config = " config)]
    (when-not (affirm-loaded?)
      (tags/insert-tag-with-text (str assignment-var "; " affirm-anon-fn) "affirm")
      (when-not (affirm-loaded?) ;; incase affirm js doesn't load
        (js/affirm.ui.ready
         (fn insertOnReady []
           (if (.hasOwnProperty js/affirm.checkout "on")
             (js/affirm.checkout.on
              "error"
              (fn [err]
                (handle-message events/affirm-checkout-error (js->clj err :keywordize-keys true))))
             (js/affirm.ui.error.on
              "close"
              (fn []
                (handle-message events/affirm-ui-error-closed))))))))))

(defn refresh []
  (when (affirm-loaded?)
    (js/affirm.ui.ready #(js/affirm.ui.refresh))))

(defn has-openModal? []
  (.hasOwnProperty js/affirm.ui "openModal"))

(defn show-site-modal []
  (if (has-openModal?)
    (js/affirm.ui.openModal (clj->js {:id "promo_set_pdp"}))
    (throw (ex-info "affirm.ui.openModal is missing!" {}))))

(defn show-modal []
  (when (affirm-loaded?)
    (js/affirm.ui.ready show-site-modal)))

(defn checkout [affirm-order]
  (when (affirm-loaded?)
    (js/affirm.ui.ready
     (fn checkoutOnReady []
       (js/affirm.checkout (clj->js affirm-order))
       (js/affirm.checkout.open)))))
