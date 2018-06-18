(ns mayvenn-made.home
  (:require [storefront.component :as component]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.components.ui :as ui]))

(defn hero-image [{:keys [desktop-url mobile-url file-name alt]}]
  [:picture
   ;; Tablet/Desktop
   [:source {:media   "(min-width: 750px)"
             :src-set (str desktop-url "-/format/auto/-/quality/best/" file-name " 1x")}]
   ;; Mobile
   [:img.block.col-12 {:src (str mobile-url "-/format/auto/" file-name)
                       :alt alt}]])

(defn component [{:keys [query/hero]} opts]
  (component/create
   [:section (hero-image hero)]))

(defn query [data]
  {:query/hero {:desktop-url "//ucarecdn.com/75da64bd-b00f-465a-bfb2-b3c0b5ac34cd/"
                :mobile-url  "//ucarecdn.com/af86155d-5960-4f7c-8ecc-817c27b81269/"
                :file-name   "mayvenn-made.png"
                :alt         "Share your best #mayvennmade looks for a chance to be featured"}})

(defn built-component [data opts]
  (component/build component (query data) opts))

(defmethod effects/perform-effects events/navigate-mayvenn-made
  [dispatch event args prev-app-state app-state])
