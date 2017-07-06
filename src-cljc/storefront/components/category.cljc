(ns storefront.components.category
  (:require [storefront.platform.component-utils :as utils]
            [storefront.components.money-formatters :refer [as-money-without-cents as-money]]
            [storefront.accessors.promos :as promos]
            [storefront.accessors.named-searches :as named-searches]
            [storefront.accessors.categories :as categories]
            [storefront.accessors.products :as products]
            [storefront.accessors.orders :as orders]
            [storefront.accessors.experiments :as experiments]
            [storefront.accessors.bundle-builder :as bundle-builder]
            [storefront.platform.reviews :as reviews]
            [storefront.platform.ugc :as ugc]
            [storefront.components.ui :as ui]
            [clojure.string :as string]
            [clojure.set :as set]
            #?(:clj [storefront.component-shim :as component]
               :cljs [storefront.component :as component])
            [storefront.events :as events]
            [storefront.keypaths :as keypaths]
            [storefront.assets :as assets]
            [storefront.request-keys :as request-keys]
            [storefront.platform.carousel :as carousel]))

(defn ^:private component [{:keys [category]} owner opts]
  (component/create
   [:div
    (let [{:keys [mobile-url file-name desktop-url alt]} (:hero (:images category))]
      [:picture
       [:source {:media "(min-width: 750px)"
                 :src-set (str desktop-url "-/format/auto/" file-name " 1x")}]
       [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                           :alt alt}]])
    [:p.h3.my4.max-580.mx-auto.center (-> category :copy :description)]]))

(defn ^:private query [data]
  {:category (get-in data keypaths/current-category)})

(defn built-component [data opts]
  (component/build component (query data) opts))
