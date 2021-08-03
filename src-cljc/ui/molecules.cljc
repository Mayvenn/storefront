(ns ui.molecules
  (:require [storefront.component :as component :refer [defcomponent]]
            [storefront.components.svg :as svg]
            [storefront.components.ui :as ui]
            [storefront.components.formatters :as formatters]
            [storefront.platform.component-utils :as utils]
            [mayvenn.concept.booking :as booking]))

(defn return-link
  [{:return-link/keys [copy id back]
    [event & args]    :return-link/event-message}]
  (component/html
   (when id
     [:div.proxima.title-3.shout.bold.flex
      [:a.inherit-color
       (merge {:data-test id}
              (if (= :navigate (first event))
                (apply utils/route-back-or-to back event args)
                (apply utils/fake-href event args)))
       [:div.flex.line-height-1
        (ui/back-caret {})
        [:span.ml1.border-bottom.border-black.border-width-3 copy]]]])))

;; TODO move/refactor to remove destructuring?
(defn stars-rating-molecule
  [{:rating/keys [value id]}]
  (component/html
   (when (and id value)
     (let [{:keys [whole-stars partial-star empty-stars]} (ui/rating->stars value "13px")]
       [:div.flex.items-center.button-font-3.s-color
        {:data-test id}
        whole-stars
        partial-star
        empty-stars]))))

(defn ^:private ucare-hero
  [mob-uuid dsk-uuid file-name alt]
  (let [mobile-url  (str "//ucarecdn.com/" mob-uuid "/-/format/auto/-/")
        desktop-url (str "//ucarecdn.com/" (or dsk-uuid mob-uuid) "/-/format/auto/-/")]
    (component/html
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
                          :alt (str alt)}]])))

(defn ^:private image-hero
  [mob-url dsk-url alt]
  (component/html
   [:picture
    ;; Tablet/Desktop
    (for [img-type                      ["webp" "jpg"]
          [media-query img-url src-set] [["(min-width: 750px)" dsk-url {"1x" {:w "1600"
                                                                              :q "75"}}]
                                         ["(min-width: 426px)" mob-url {"1x" {:w "750"
                                                                              :q "75"}
                                                                        "2x" {:w "1500"
                                                                              :q "50"}}]
                                         ["(min-width: 321px)" mob-url {"1x" {:w "425"
                                                                              :q "75"}
                                                                        "2x" {:w "850"
                                                                              :q "50"}}]
                                         [nil mob-url {:src-set {"1x" {:w "320"
                                                                       :q "75"}
                                                                 "2x" {:w "640"
                                                                       :q "50"}}
                                                       :type    img-type}]]]
      (ui/source img-url
                 {:media   media-query
                  :src-set {"1x" {:w "1600"
                                  :q "75"}}
                  :type    img-type}))
    [:img.block.col-12 {:src mob-url
                        :alt alt}]]))

(defcomponent hero
  [{:keys [opts
           dsk-uuid
           mob-uuid
           dsk-url
           mob-url
           file-name
           alt
           ucare?
           off-screen?]
    :or   {file-name "hero-image"}} _ _]
  [:a (cond-> opts
        (:navigation-message opts)
        (-> (merge (apply utils/route-to (:navigation-message opts)))
            (dissoc :navigation-message)))
   (cond
     off-screen? [:div.col-12]
     ucare?      (ucare-hero mob-uuid dsk-uuid file-name alt)
     :else       (image-hero mob-url dsk-url alt))])

(defn field-reveal-molecule
  [{:field-reveal/keys [id label target]}]
  (when id
    (component/html
     [:a.mlp3.content-3 ^:attrs (merge {:data-test id}
                                       (apply utils/fake-href target))
      label])))

(defn ^:private star [index type]
  (component/html
   [:span.mrp1
    {:key (str (name type) "-" index)}
    (case type
      :whole         (svg/whole-star         {:height "13px" :width "13px"})
      :three-quarter (svg/three-quarter-star {:height "13px" :width "13px"})
      :half          (svg/half-star          {:height "13px" :width "13px"})
      :empty         (svg/empty-star         {:height "13px" :width "13px"})
      nil)]))

(defn ^:private rating->stars
  [rating full-rating]
  (when (pos? full-rating)
    (conj
     (rating->stars (dec rating) (dec full-rating))
     (condp <= rating
       1    :whole
       0.75 :three-quarter
       0.50 :half
       :empty))))

(defn svg-star-rating
  [rating]
  (component/html
   [:div.flex.items-center
    [:span.mrp2 rating]
    (map-indexed star (rating->stars rating 5))]))

(defn svg-star-rating-molecule
  [{:rating/keys [value]}]
  (component/html
   [:div.content-3.s-color
    (svg-star-rating value)]))

(defn stylist-appointment-time [{:keys [date slot-id]}]
  ;; TODO(ellie, 2021-08-02): We are unconvinced this belongs here.
  (let [date-copy #?(:clj nil
                     :cljs (formatters/long-date date))
        time-copy (->> booking/time-slots
                       (filter (fn [{:slot/keys [id]}]
                                 (= id slot-id)))
                       first
                       :slot.card/copy)
        copy      (when (and (seq date-copy)
                             (seq time-copy))
                    (str date-copy " at " time-copy))]
    (when copy
      (component/html
       [:div.content-3.pt1.flex
        (svg/calendar {:class  "mr1 fill-p-color"
                       :width  "1.1em"
                       :height "1.1em"})
        [:div.pb1 copy]]))))
