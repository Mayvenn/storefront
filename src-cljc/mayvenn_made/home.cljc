(ns mayvenn-made.home
  (:require #?@(:cljs [[storefront.hooks.pixlee :as pixlee]
                       [storefront.api :as api]
                       [om.core :as om]])
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.platform.component-utils :as utils]))

(defn hero-image
  [{:as hero-map :keys [desktop mobile alt]}]
  (let [mobile-url  (-> mobile :file :url)
        desktop-url (-> desktop :file :url)]
    [:a (utils/fake-href events/control-mayvenn-made-hero-clicked)
     (conj (into [:picture]
                   (for [img-type    ["webp" "jpg"]
                         [url media] [[desktop-url "(min-width: 750px)"]
                                      [mobile-url nil]]]
                     (ui/source url
                                {:media   media
                                 :src-set {"1x" {}}
                                 :type    img-type})))
           [:img.block.col-12 {:src mobile-url :alt alt}])]))

(defn component [{:keys [image/hero]} owner opts]
  (component/create
   [:div
    [:section (hero-image hero)]]))

(defn query
  [data]
  {:image/hero (get-in data keypaths/cms-mayvenn-made-hero)})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/control-mayvenn-made-hero-clicked
  [_ _ _ _ _]
  #?(:cljs (pixlee/open-uploader)))

(defmethod effects/perform-effects events/navigate-mayvenn-made
  [_ _ _ _ app-state]
  #?(:cljs
     (do
       (effects/fetch-cms-data app-state {:slices [:mayvennMadePage]})
       (pixlee/insert))))

(defmethod effects/perform-effects events/inserted-pixlee
  [_ _ _ _ _]
  #?(:cljs (pixlee/add-simple-widget)))
