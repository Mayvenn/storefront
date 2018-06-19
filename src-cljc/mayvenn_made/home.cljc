(ns mayvenn-made.home
  (:require #?@(:cljs [[storefront.hooks.pixlee :as pixlee]
                       [om.core :as om]])
            [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            [storefront.transitions :as transitions]
            [storefront.platform.messages :as messages]))

(defn hero-image
  [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn simple-widget-component
  [data owner opts]
  #?(:cljs (reify
             om/IRender
             (render [_]
               (component/html [:div]))

             om/IDidMount
             (did-mount [this]
               (messages/handle-message events/mayvenn-made-gallery-displayed)))))

(defn component [{:keys [query/hero]} owner opts]
  (component/create
   [:div
    [:section (hero-image hero)]
    [:section
     [:div#pixlee_container]
     #?(:cljs (om/build simple-widget-component {}))]]))


(defn query
  [data]
  {:query/hero {:desktop-url "//ucarecdn.com/75da64bd-b00f-465a-bfb2-b3c0b5ac34cd/"
                :mobile-url  "//ucarecdn.com/af86155d-5960-4f7c-8ecc-817c27b81269/"
                :file-name   "mayvenn-made.png"
                :alt         "Share your best #mayvennmade looks for a chance to be featured"}})

(defn built-component
  [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-mayvenn-made
  [_ _ _ _ _]
  #?(:cljs (pixlee/insert)))

(defmethod effects/perform-effects events/mayvenn-made-gallery-displayed
  [_ _ _ _ _]
  #?(:cljs (pixlee/add-simple-widget)))

(defmethod effects/perform-effects events/inserted-pixlee
  [_ _ _ _ _]
  #?(:cljs (pixlee/add-simple-widget)))
