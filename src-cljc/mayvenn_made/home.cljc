(ns mayvenn-made.home
  (:require #?@(:cljs [[storefront.hooks.pixlee :as pixlee]
                       [om.core :as om]])
            [storefront.component :as component :refer [defcomponent]]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]
            [storefront.platform.component-utils :as utils]
            
            
            [storefront.component :as component :refer [defcomponent]]
            [storefront.component :as component :refer [defcomponent]]))


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

(defn simple-widget-component
  [data owner opts]
  #?(:cljs (reify
             om/IRender
             (render [_]
               (component/html [:div]))

             om/IDidMount
             (did-mount [this]
               (messages/handle-message events/mayvenn-made-gallery-displayed)))))

(defcomponent component [{:keys [image/hero]} owner opts]
  [:div
    [:section (hero-image hero)]
    [:section [:h1.p8.bold.shout.center "Recently submitted looks"]]
    [:section
     [:div#pixlee_container]
     #?(:cljs (component/build simple-widget-component {}))]])


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
  [_ _ _ _ _]
  #?(:cljs (pixlee/insert)))

(defmethod effects/perform-effects events/mayvenn-made-gallery-displayed
  [_ _ _ _ _]
  #?(:cljs (pixlee/add-simple-widget)))

(defmethod effects/perform-effects events/inserted-pixlee
  [_ _ _ _ _]
  #?(:cljs (pixlee/add-simple-widget)))
