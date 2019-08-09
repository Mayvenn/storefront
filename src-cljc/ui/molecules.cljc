(ns ui.molecules
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]))

(defn return-link
  [{:return-link/keys [copy id back]
    [event & args]    :return-link/event-message}]
  [:div.h6.medium.navy
   (when id {:data-test id})
   [:a.inherit-color (if (= :navigate (first event))
                       (apply utils/route-back-or-to back event args)
                       (apply utils/fake-href event args))
    (ui/back-caret copy "12px")]])
