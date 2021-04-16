(ns mayvenn.visual.lib.call-out-box
  "Call out boxes"
  (:require [storefront.component :as c]
            [mayvenn.visual.ui.actions :as actions]
            [mayvenn.visual.ui.titles :as titles]
            [storefront.platform.component-utils :as utils]))

(defn with
  [key data]
  (let [ks (filter #(= (name key) (namespace %))
                   (keys data))]
    (into {}
          (map (fn [[k v]] [(-> k name keyword) v]))
          (select-keys data ks))))

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

;; Pale purple, Canela
(c/defcomponent variation-1
  [data _ _]
  (when (seq data)
    [:div.m3
     [:div.col-12.bg-pale-purple.flex.flex-column.items-center.p5
      (link-to-atom
       (with :action data)
       (titles/canela (with :title data)))
      (actions/action-molecule (with :action data))]]))

;; Warm Gray, Proxima
(c/defcomponent variation-2
  [data _ _]
  (when (seq data)
    [:div.m3
     [:div.col-12.bg-warm-gray.flex.flex-column.items-center.p5
      (link-to-atom
       (with :action data)
       (titles/proxima (with :title data)))
      (actions/action-molecule (with :action data))]]))


