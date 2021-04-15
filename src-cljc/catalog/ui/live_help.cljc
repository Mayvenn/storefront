(ns catalog.ui.live-help
  "Call out boxes"
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.platform.component-utils :as utils]))

(defn with
  [key data]
  (let [ks (filter #(= (name key) (namespace %))
                   (keys data))]
    (into {}
          (map (fn [[k v]] [(-> k name keyword) v]))
          (select-keys data ks))))

(defn- title-molecule
  [{:keys [icon primary secondary target]}]
  (c/html
   [:div.center
    (svg/symbolic->html icon)
    [:div.title-2.proxima.shout primary]
    [:div.mt2.content-2 secondary]]))

(defn- action-molecule
  [{:keys [id label target]}]
  (c/html
   [:div.mt4.col-10.col-8-on-tb
    (ui/button-medium-primary
     (merge {:data-test id}
            (apply utils/route-to target))
     label)]))

;; TODO(corey)
;; Another way to look at this is as
;; this is a wrapper around/higher order elements
;; Nice to have a general utility to apply
;; to a collection of elements including
;; - prepend
;; - append
;; - nth insert
;; - wrap
(defn link-to-atom
  [{:keys [target]} & content]
  [:a.inherit-color
   (apply utils/route-to target)
   content])

(c/defcomponent organism
  [{:as data :keys [id icon title subtitle image-ucare-id button-copy nav-event]} _ _]
  (when (seq data)
    [:div.m3
     [:div.col-12.bg-warm-gray.flex.flex-column.items-center.p5
      {:class "bg-warm-gray"}
      (link-to-atom
       (with :action data)
       (title-molecule (with :title data)))
      (action-molecule (with :action data))]]))
