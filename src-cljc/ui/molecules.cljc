(ns ui.molecules
  (:require [storefront.component :as component]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
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

(defn stars-rating-molecule
  [{rating :rating/value}]
  (when rating
    (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars rating)]
      [:div.h5.flex.items-center.line-height-2
       [:span.orange.bold.mr1 rating]
       whole-stars
       partial-star
       empty-stars])))

(defn hero
  [{:keys [dsk-uuid mob-uuid file-name alt opts off-screen?]
    :or   {file-name "hero-image"}} _ _]
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

(defn labeled-input-molecule
  [{:labeled-input/keys [id label value on-change]}]
  (when id
    [:input.h5.border-none.px2.bg-white.placeholder-dark-silver.flex-grow-1
     {:key         id
      :data-test   id
      :name        id
      :id          id
      :type        "text"
      :value       (or value "")
      :label       label
      :placeholder label
      :on-submit   on-change
      :on-change   on-change}]))

(defn submit-button-molecule
  [{:submit-button/keys [id contents target classes disabled?]}]
  (ui/teal-button
   (merge {:style          {:width   "40px"
                            :height  "40px"
                            :padding "0"}
           :width          :small
           :disabled?      disabled?
           :disabled-class "bg-light-gray gray"
           :data-test      id
           :class          "dark-gray flex medium not-rounded items-center justify-center"}
          (utils/fake-href target))
   (svg/forward-arrow {:disabled? disabled?
                       :style     {:width  "14px"
                                   :height "14px"}})))

(defn input-group-field-and-button-molecule
  [data]
  [:div.bg-white.border.border-light-gray.rounded.overflow-hidden.table.flex.col-12
   (labeled-input-molecule data)
   (submit-button-molecule data)])
