(ns storefront.analytics
  (:require [storefront.script-tags :refer [insert-tag-with-text remove-tag remove-tag-by-src]]
            [storefront.config :as config]
            [clojure.string :as s]))

(defn insert-tracking []
  (insert-tag-with-text
   (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
 (i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
 m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
 })(window,document,'script','//www.google-analytics.com/analytics.js','ga');

ga('create', '" config/google-analytics-property "', 'auto');
ga('require', 'displayfeatures');
ga('require', 'ec');")
   "analytics")
  (insert-tag-with-text
   "(function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    '//www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-TLS2JL');"
   "tag-manager")
  )

(defn remove-tracking []
  (remove-tag "analytics")
  (remove-tag "tag-manager")
  ;; ga inserts more tags (as expected); remove them to help prevent so many additional ones in development
  (remove-tag-by-src "//www.google-analytics.com/analytics.js")
  (remove-tag-by-src "//www.googletagmanager.com/gtm.js?id=GTM-TLS2JL"))

(defn- ->ga-product [product & [event-fields]]
  (merge (select-keys product [:name :price :category])
         {:brand (s/lower-case (str "mayvenn-" (:collection_name product) "-hair"))
          :id (or (:master_sku product) (-> product :master :sku))}
         event-fields))

(defn add-impression [product & [event-fields]]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "ec:addImpression" (clj->js (->ga-product product event-fields)))))

(defn add-product [product & [event-fields]]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "ec:addProduct" (clj->js (->ga-product product event-fields)))))

(defn add-line-items [line-items]
  (doseq [line-item line-items]
    (let [variant (:variant line-item)]
      (add-product (select-keys variant
                                [:master_sku :name :price :category :collection_name])
                   {:quantity (:quantity line-item) :variant (:sku variant)}))))

(defn set-action [action & args]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "ec:setAction" action (clj->js (apply hash-map args)))))

(defn set-checkout-step [step line-items]
  (add-line-items line-items)
  (set-action "checkout" :step step))

(defn set-purchase [order]
  (add-line-items (:line_items order))
  (set-action "purchase"
              :id (:number order)
              :affiliation "Mayvenn"
              :revenue (:total order)
              :tax (:tax_total order)
              :shipping (:ship_total order)))

(defn track-event [category action & [label value non-interaction]]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "send"
           "event"
           category
           action
           label
           value
           (when non-interaction (clj->js {"nonInteraction" (str non-interaction)})))))

(defn track-checkout-option [step option]
  (set-action "checkout_option" :step step :option option)
  (track-event "Checkout" "Option"))

(defn track-page [path]
  (when (.hasOwnProperty js/window "ga")
    (js/ga "set" "page" (clj->js path))
    (js/ga "send" "pageview")))
