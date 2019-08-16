(ns ui.molecules
  (:require [storefront.components.ui :as ui]
            [storefront.platform.component-utils :as utils]
            [storefront.component :as component]))

(defn return-link
  [{:return-link/keys [copy id back]
    [event & args]    :return-link/event-message}]
  [:div.h6.medium.navy
   (when id {:data-test id})
   [:a.inherit-color (if (= :navigate (first event))
                       (apply utils/route-back-or-to back event args)
                       (apply utils/fake-href event args))
    (ui/back-caret copy "12px")]])

(defn hero
  [{:keys [dsk-uuid mob-uuid file-name alt opts off-screen?]
    :or   {file-name "hero-image"}}]
  (let [mobile-url  (str "//ucarecdn.com/" mob-uuid "/-/format/auto/-/")
        desktop-url (str "//ucarecdn.com/" (or dsk-uuid mob-uuid) "/-/format/auto/-/")]
    (component/create
     [:a
      opts
      (if off-screen?
        [:picture
         ;; Tablet/Desktop
         [:source {:media   "(min-width: 750px)"
                   :src-set (str desktop-url "quality/best/-/resize/720x/" file-name " 1x")}]
         ;; Mobile
         [:source {:media   "(min-width: 426px)"
                   :src-set (str mobile-url "quality/lightest/-/resize/425x/" file-name " 1x ")}]
         [:source {:src-set (str mobile-url "quality/lightest/-/resize/425x/" file-name " 1x ")}]
         ;; mobile
         [:img.block.col-12 {:src (str mobile-url "quality/lightest/-/resize/425x/" file-name)
                             :alt (str alt)}]]

        ;; ON-SCREEN
        [:picture
         ;; Tablet/Desktop
         [:source {:media   "(min-width: 750px)"
                   :src-set (str desktop-url "quality/best/-/resize/1440x/" file-name " 1x")}]
         ;; Mobile
         [:source {:media   "(min-width: 426px)"
                   :src-set (str mobile-url "quality/lightest/-/resize/2250x/" file-name " 3x, "
                                 mobile-url "quality/lightest/-/resize/1500x/" file-name " 2x, "
                                 mobile-url "quality/normal/-/resize/750x/" file-name " 1x ")}]
         [:source {:src-set (str mobile-url "quality/lightest/-/resize/1275x/" file-name " 3x, "
                                 mobile-url "quality/lightest/-/resize/850x/" file-name " 2x, "
                                 mobile-url "quality/normal/-/resize/425x/" file-name " 1x ")}]
         ;; mobile
         [:img.block.col-12 {:src (str mobile-url "quality/normal/-/resize/750x/" file-name)
                             :alt (str alt)}]])])))
