(ns ui.molecules
  (:require [storefront.component :as component :refer [defcomponent]]
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

(defn ^:private ucare-hero
  [mob-uuid dsk-uuid file-name alt]
  (let [mobile-url  (str "//ucarecdn.com/" mob-uuid "/-/format/auto/-/")
        desktop-url (str "//ucarecdn.com/" (or dsk-uuid mob-uuid) "/-/format/auto/-/")]
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
                         :alt (str alt)}]]))

(defn ^:private image-hero
  [mob-url dsk-url alt]
  [:picture
   ;; Tablet/Desktop
   (for [img-type ["webp" "jpg"]]
     [(ui/source dsk-url
                 {:media   "(min-width: 750px)"
                  :src-set {"1x" {:w "1600"
                                  :q "75"}}
                  :type    img-type})
      (ui/source mob-url
                 {:media   "(min-width: 426px)"
                  :src-set {"1x" {:w "750"
                                  :q "75"}
                            "2x" {:w "1500"
                                  :q "50"}}
                  :type    img-type})
      (ui/source mob-url
                 {:media   "(min-width: 321px)"
                  :src-set {"1x" {:w "425"
                                  :q "75"}
                            "2x" {:w "850"
                                  :q "50"}}
                  :type    img-type})
      (ui/source mob-url
                 {:src-set {"1x" {:w "320"
                                  :q "75"}
                            "2x" {:w "640"
                                  :q "50"}}
                  :type    img-type})])
   [:img.block.col-12 {:src mob-url
                       :alt alt}]])

(defcomponent hero
  [{:keys [dsk-uuid dsk-url mob-uuid mob-url file-name alt opts off-screen?]
    :or   {file-name "hero-image"}} _ _]
  (let [ucare? (boolean mob-uuid)]
    [:a opts (cond
               off-screen? [:div.col-12]
               ucare?      (ucare-hero mob-uuid dsk-uuid file-name alt)
               :else       (image-hero mob-url dsk-url alt))]))

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
   (svg/forward-arrow {:style {:width  "14px"
                               :height "14px"}})))

(defn input-group-field-and-button-molecule
  [{:submit-button/keys [target disabled?] :as data}]
  [:form.bg-white.border.border-light-gray.rounded.overflow-hidden.table.flex.col-12
   (when-not disabled?
     {:on-submit (utils/send-event-callback target)})
   (labeled-input-molecule data)
   (submit-button-molecule data)])

(defn ^:private star [index type]
  [:span.mrp1
   {:key (str (name type) "-" index)}
   (case type
     :whole         (svg/whole-star         {:height "13px" :width "13px"})
     :three-quarter (svg/three-quarter-star {:height "13px" :width "13px"})
     :half          (svg/half-star          {:height "13px" :width "13px"})
     :empty         (svg/empty-star         {:height "13px" :width "13px"})
     nil)])

(defn ^:private rating->stars
  [rating full-rating]
  (when (pos? full-rating)
    (conj
     (rating->stars (dec rating) (dec full-rating))
     (condp <= rating
       1    :whole
       0.75 :three-quarter
       0.50 :half-star
       :empty))))

(defn svg-star-rating
  [rating]
  [:div.flex.items-center
   [:span.mrp2 rating]
   (map-indexed star (rating->stars rating 5))])

(defn svg-star-rating-molecule
  [{:rating/keys [value]}]
  (component/html
   [:div.h6.orange
    (svg-star-rating value)]))
