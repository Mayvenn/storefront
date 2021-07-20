(ns mayvenn.visual.ui.actions
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

;; TODO(corey) only takes nav targets
;; TODO(corey) disabled states
;; TODO(corey) how does this interact with ui buttons
;;             probably supercedes

(defn action-molecule
  [{:keys [id label target]}]
  (when id
    (c/html
     [:div.mt4.col-10.col-8-on-tb
      (ui/button-medium-primary
       (merge {:data-test id}
              (apply utils/route-to target))
       label)])))

(defn medium-primary
  [{:keys [id disabled? label target]}]
  (when id
    (c/html
     [:div
      (ui/button-medium-primary
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (apply utils/route-to target))
       label)])))

(defn large-primary
  [{:keys [id disabled? label target]}]
  (when id
    (c/html
     [:div.col-10.col-8-on-tb
      (ui/button-large-primary
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (apply utils/route-to target))
       label)])))

(defn large-paypal
  [{:keys [id disabled? spinning? target]}]
  (when id
    (c/html
     [:div.col-10.col-8-on-tb
      (ui/button-large-paypal
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (when spinning? {:spinning? spinning?})
              {:on-click (apply utils/send-event-callback target)})
       (c/html
        [:div
         "Check out with "
         [:span.medium.italic "PayPal™"]]))])))

(defn small-primary
  [{:keys [id disabled? label target]}]
  (when id
    (c/html
     [:div.col-10.col-8-on-tb
      (ui/button-small-primary
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (apply utils/route-to target))
       label)])))

(defn small-secondary
  [{:keys [id disabled? label target]}]
  (when id
    (c/html
     [:div.col-10.col-8-on-tb
      (ui/button-small-secondary
       (merge {:data-test id}
              (when disabled? {:disabled? disabled?})
              (apply utils/route-to target))
       label)])))

(defn medium-tertiary
  [{:keys [id disabled? label target]}]
  (when id
    (c/html
     [:div.center
      [:a.p-color.center.title-2.shout.bold.underline.proxima
       (merge {:data-test id}
              (apply utils/route-to target))
       label]])))
