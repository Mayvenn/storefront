(ns storefront.components.email-capture
  (:require [storefront.accessors.experiments :as experiments]
            [mayvenn.concept.email-capture :as concept]
            [storefront.browser.scroll :as scroll]
            [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.components.svg :as svg]
            [storefront.events :as e]
            [storefront.keypaths :as k]
            [storefront.platform.messages :as messages
             :refer [handle-message]
             :rename {handle-message publish}]
            [storefront.platform.component-utils :as utils]))

(defn m-header [id close-dialog-href]
  [:div.flex.justify-between.items-center.p2.bg-white
   [:div]
   [:div.flex.justify-center.pt1
    ^:inline (svg/mayvenn-logo {:width "52px" :height "30px"})]
   (ui/modal-close {:data-test   (str id "-dismiss")
                    :class       "fill-black stroke-black"
                    :close-attrs close-dialog-href})])

(defn bg-image
  [{:email-capture.photo/keys [uuid-mob uuid-dsk]}]
  (c/html
   (if (not= uuid-mob uuid-dsk)
     [:div
      [:div.hide-on-mb
       (ui/aspect-ratio 1 1
                        (ui/img {:max-size "500px"
                                 :class    "col-12"
                                 :style    {:vertical-align "bottom"}
                                 :src      uuid-dsk
                                 ;; no alt for decorative image
                                 :alt      ""}))]
      [:div.hide-on-tb-dt
       (ui/aspect-ratio 4 3
                        (ui/img {:max-size "500px"
                                 :class    "col-12"
                                 :style    {:vertical-align "bottom"}
                                 :src      uuid-mob
                                 ;; no alt for decorative image
                                 :alt      ""}))]]
     (ui/aspect-ratio 4 3
                      (ui/img {:max-size "500px"
                               :class    "col-12"
                               :style    {:vertical-align "bottom"}
                               :src      uuid-mob
                               ;; no alt for decorative image
                               :alt      ""})))))

(defn title [{:email-capture.title/keys [primary]}]
  [:div.proxima.title-1.mb3
   primary])

(def hr-divider
  [:hr.border-top.border-gray.col-12.m0
   {:style {:border-bottom 0 :border-left 0 :border-right 0}}])

(defn text-field [{:email-capture.text-field/keys [id placeholder focused keypath errors email]}]
  [:div.mx-auto.mb3
   (ui/text-field {:errors    (get errors ["email"])
                   :keypath   keypath
                   :focused   focused
                   :label     placeholder
                   :name      "email"
                   :required  true
                   :type      "email"
                   :value     email
                   :class     "col-12 bg-white"
                   :data-test id})])

(def fine-print-1
  [:div.px2.pt4
   {:style {:color "#767676"
            :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
   "*$25 Off offer is for first-time subscribers only. $200 minimum purchase required. "
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn cta-1 [{:email-capture.cta/keys [value id]}]
  [:div.mb5 (ui/submit-button-medium value {:data-test id})])

;; TODO consider using c/build and c/html and stuff
(defn design-first-pageview-email-capture-2021-non-bf [{:keys [id]
                 :as   data}]
  [:div.flex.flex-column
   {:data-test (str id "-modal")}
   (m-header id (apply utils/fake-href (:email-capture.dismiss/target data)))
   (bg-image data)
   [:div.p4.black.bg-white
    [:form.col-12.center.px1
     {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
     (title data)
     [:div.px3
      (text-field data)
      (cta-1 data)]]
    hr-divider
    fine-print-1]])

(defn cta-2 [{:email-capture.cta/keys [value id]}]
  [:div.mb5 (ui/submit-button-medium value {:data-test id})])

(def fine-print-2
  [:div.px2.pt4.pb6
   {:style {:color "#6b6b6b"
            :font "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   "*$35 Off offer is for first-time subscribers only. $200 minimum purchase required. "
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(def fine-print-4
  [:div.px2.pt4.pb6
   {:style {:color "#6b6b6b"
            :font "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   "*$50 Off offer is for first-time subscribers only. $300 minimum purchase required. "
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn fine-print-5 [prefix]
  [:div.px2.pt4.pb6
   {:style {:color "#6b6b6b"
            :font  "12px/17px 'Proxima Nova', Arial, sans-serif"}}
   prefix
   " For further information, please read our "
   [:a.p-color (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.p-color (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn design-first-pageview-email-capture-2022-04
  [{:keys [id] :as   data}]
  (c/html
   [:div.flex.flex-column
    {:data-test (str id "-modal")}
    (m-header id (apply utils/fake-href (:email-capture.dismiss/target data)))
    (bg-image data)
    [:div.p4.black.bg-cool-gray
     [:form.col-12.center.px1
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (title data)
      [:div.px3
       (text-field data)
       (cta-1 data)]]
     hr-divider
     fine-print-4]]))

(defn design-adv-quiz-email-capture-2021-non-bf [{:keys [id] :as data}]
  [:div.bg-pale-purple.p4
   {:data-test (str id "-modal")}
   [:div.flex.justify-end
    (ui/modal-close {:data-test   (str id "-dismiss")
                     :class       "fill-black stroke-black"
                     :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]
   [:div.flex.justify-center.my10
    ^:inline (svg/mayvenn-logo {:width "83px" :height "48px"})]
   [:div.center.canela.title-1.mb6 "Looking For A Stylist?"]
   [:div.center.mb6
    {:style {:font "32px/37px 'Proxima Nova'"
             :letter-spacing "0.33px"}}
    "Sign up to receive top-rated stylists near you, promotional offers, and for a limited time, get "
    [:span.p-color "$35 OFF"]
    " for joining."]
   [:form.col-12.center.px4
    {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
    (text-field data)
    (cta-2 data)]
   hr-divider
   fine-print-2])

;; Used for Early Access
(def fine-print-3
  [:div.px2.pt4
   {:style {:color "white"
            :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
   "Mayvenn may choose to modify the promotion at any time. "
   "*I consent to receive Mayvenn marketing content via email. "
   "For further information, please read our "
   [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
   " and "
   [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
   ". Unsubscribe anytime."])

(defn design-first-pageview-email-capture-2021-pre-bf-lower-text [data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "b3537c0c-a339-4cd1-9518-5ba1576b8b8c")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.px6.mx-auto.mynp6.pb8
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.pt4.shout.bold.title-1 "Just for you"]
    [:div.canela.title-1.pb2.yellow
     "$20 Off &" [:br]
     "Black Friday Deals"]
    [:div.shout.bold.title-1 "Early Subscriber Access"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]]
     [:div.content-2
      [:div.my2 "Join our email list to get $20 off and exclusive early access to our Black Friday deals."]
      [:div.my2 "Stock up on dreamy hair at our best prices."]]
     fine-print-3]]])

(defn design-adv-quiz-email-capture-2021-pre-bf-lower-text [data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "0400b18e-7b0a-4835-8e1a-4fc2c51ae604")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-black stroke-black"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.px6.mx-auto.mynp6.pb8
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.pt4.shout.bold.title-1 "Just for you"]
    [:div.canela.title-1.pb2.yellow
     "$20 Off &" [:br]
     "Black Friday Deals"]
    [:div.shout.bold.title-1 "Early Subscriber Access"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]]
     [:div.content-2
      [:div.my2 "Join our email list to get $20 off and exclusive early access to our Black Friday deals."]
      [:div.my2 "Stock up on dreamy hair at our best prices."]]
     fine-print-3]]])

(defn design-first-pageview-email-capture-2021-bf [discount-amount data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "e24dbb4a-7cbf-4479-8bb4-57bebe180c29")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.mx-auto.mynp6.py8.px3
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.canela.title-1.pb2.yellow
     (str "Enjoy $" discount-amount " Off")]
    [:div.shout.proxima.title-3.mb6 "and get access to exclusive offers!"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]]]
    [:div.pt4
     {:style {:color "white"
              :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
     "Early access to our Black Friday deals."
     "*I consent to receive Mayvenn marketing content via email. "
     "For further information, please read our "
     [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
     " and "
     [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
     ". Unsubscribe anytime."]]
   [:div.bg-white.p4.center
    (svg/mayvenn-logo {:style {:width "50px" :height "30px"}})]])

(defn design-adv-quiz-email-capture-2021-bf [discount-amount data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "3a24b338-b84e-410a-913f-5416d3c95ad6")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.mx-auto.mynp6.py8.px3
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.canela.title-1.pb2.yellow
     (str "Enjoy $" discount-amount " Off")]
    [:div.shout.proxima.title-3.mb6 "and get access to exclusive offers!"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]]]
    [:div.pt4
     {:style {:color "white"
              :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
     "Early access to our Black Friday deals."
     "*I consent to receive Mayvenn marketing content via email. "
     "For further information, please read our "
     [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
     " and "
     [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
     ". Unsubscribe anytime."]]
   [:div.bg-white.p4.center
    (svg/mayvenn-logo {:style {:width "50px" :height "30px"}})]])

(defn design-first-pageview-email-capture-2021-hol [discount-amount data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "7352c0fa-0cda-4dbf-8ddf-d5ebeb1b2f98")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.mx-auto.mynp6.py8.px3
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.shout.proxima.title-3 "Stay in the know"]
    [:div.canela.title-1.pb2.yellow
     (str "Sign Up For $" discount-amount " Off")]
    [:div.proxima.title-3.mb6 "and get the latest holiday offers!"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Subscribe Now" {:data-test "email-capture-submit"})]]]
    [:div.pt4
     {:style {:color "white"
              :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
     "Early access to our Black Friday deals."
     "*I consent to receive Mayvenn marketing content via email. "
     "For further information, please read our "
     [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
     " and "
     [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
     ". Unsubscribe anytime."]]
   [:div.bg-white.p4.center
    (svg/mayvenn-logo {:style {:width "50px" :height "30px"}})]])

(defn design-adv-quiz-email-capture-2021-hol [discount-amount data]
  [:div
   {:style     {:max-width "580px"}
    :data-test "email-capture-modal"}
   [:div.relative
    (ui/ucare-img {:style {:max-width "580px"}
                   ;; no alt text for decorative image
                   :alt ""} "cfeaed5f-d79a-409d-bd5b-842a98820423")
    [:div.absolute
     {:style {:top "1rem" :right "1rem"}}
     (ui/modal-close {:data-test   "email-capture-dismiss"
                      :class       "fill-white stroke-white"
                      :close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))})]]
   [:div.white.center.mx-auto.mynp6.py8.px3
    {:style {:background-image "url('//ucarecdn.com/bf5c99de-3829-465d-a066-231dcdc00ac9/-/format/auto/-/quality/lightest/-/resize/580x/')"}}
    [:div.canela.title-1.pb2.yellow
     (str "Enjoy $" discount-amount " Off")]
    [:div.proxima.title-3.mb6 "and get access to exclusive holiday offers!"]
    [:div.m3
     [:form
      {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
      (text-field data)
      [:div.my2 (ui/submit-button-medium "Sign Up Now" {:data-test "email-capture-submit"})]]]
    [:div.pt4
     {:style {:color "white"
              :font  "10px/16px 'Proxima Nova', Arial, sans-serif"}}
     "Early access to our Black Friday deals."
     "*I consent to receive Mayvenn marketing content via email. "
     "For further information, please read our "
     [:a.yellow (utils/route-to e/navigate-content-tos) "Terms"]
     " and "
     [:a.yellow (utils/route-to e/navigate-content-privacy) "Privacy Policy"]
     ". Unsubscribe anytime."]]
   [:div.bg-white.p4.center
    (svg/mayvenn-logo {:style {:width "50px" :height "30px"}})]])

(defn email-capture-modal-template-1
  [{:keys [id] :as data}]
  (c/html
   [:div.flex.flex-column
    {:data-test (str id "-modal")}
    (m-header id (apply utils/fake-href (:email-capture.dismiss/target data)))
    (let [{:email-capture.photo/keys [url title description]} data]
      (when (seq url)
        (ui/ctf-img
         {:max-width-px 500
          :url      url}
         {:title    title
          :class    "col-12"
          :style    {:vertical-align "bottom"}
          :alt      description})))
    (let [{:email-capture.copy/keys [title subtitle supertitle fine-print-lead-in]} data]
      [:div.p4.black
       {:class (:email-capture.design/background-color data)}
       [:form.col-12.center.px1
        {:on-submit (apply utils/send-event-callback (:email-capture.submit/target data))}
        [:div.mb2
         [:div.title-2.proxima.shout supertitle]
         [:div.title-1.canela.p-color title]
         [:div.title-2.proxima subtitle]]
        [:div.px3
         (text-field data)
         (cta-1 data)]]
       hr-divider
       (fine-print-5 fine-print-lead-in)])]))

(c/defdynamic-component template
  (did-mount
   [this]
   (let [])
   (scroll/disable-body-scrolling)
   (publish e/control-menu-collapse-all)
   (publish e/biz|email-capture|deployed {:id      (:capture-modal-id (c/get-props this))
                                          :variant (:email-capture/variant (c/get-props this))}))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys               [capture-modal-id]
          :email-capture/keys [variant]
          :as                 data} (c/get-props this)]
     (ui/modal
      {:close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))
       :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
       :bg-class    "bg-darken-4"}
      ((case [capture-modal-id variant]
         ["first-pageview-email-capture" "2022-05b"]               design-first-pageview-email-capture-2022-04
         ["adv-quiz-email-capture"       "2022-05b"]               design-first-pageview-email-capture-2022-04
         ["first-pageview-email-capture" "2022-05"]                design-first-pageview-email-capture-2022-04
         ["adv-quiz-email-capture"       "2022-05"]                design-first-pageview-email-capture-2022-04
         ["first-pageview-email-capture" "2021-pre-bf-lower-text"] design-first-pageview-email-capture-2021-pre-bf-lower-text
         ["adv-quiz-email-capture"       "2021-pre-bf-lower-text"] design-adv-quiz-email-capture-2021-pre-bf-lower-text
         ["first-pageview-email-capture" "2021-non-bf"]            design-first-pageview-email-capture-2021-non-bf
         ["adv-quiz-email-capture"       "2021-non-bf"]            design-adv-quiz-email-capture-2021-non-bf
         ["first-pageview-email-capture" "2021-bf-20"]             (partial design-first-pageview-email-capture-2021-bf 20)
         ["first-pageview-email-capture" "2021-bf-25"]             (partial design-first-pageview-email-capture-2021-bf 25)
         ["first-pageview-email-capture" "2021-bf-30"]             (partial design-first-pageview-email-capture-2021-bf 30)
         ["first-pageview-email-capture" "2021-hol-20"]            (partial design-first-pageview-email-capture-2021-hol 20)
         ["adv-quiz-email-capture" "2021-bf-20"]                   (partial design-adv-quiz-email-capture-2021-bf 20)
         ["adv-quiz-email-capture" "2021-bf-25"]                   (partial design-adv-quiz-email-capture-2021-bf 25)
         ["adv-quiz-email-capture" "2021-bf-30"]                   (partial design-adv-quiz-email-capture-2021-bf 30)
         ["adv-quiz-email-capture" "2021-hol-20"]                  (partial design-adv-quiz-email-capture-2021-hol 20))
       data)))))

(defn query [app-state]
  (let [nav-event               (get-in app-state k/navigation-event)
        capture-modal-id        (concept/location->email-capture-id nav-event)
        {:keys [displayable?]}  (concept/<-trigger capture-modal-id app-state)
        errors                  (get-in app-state (conj k/field-errors ["email"]))
        focused                 (get-in app-state k/ui-focus)
        textfield-keypath       concept/textfield-keypath
        email                   (get-in app-state textfield-keypath)
        variant                 (get-in app-state (conj k/features (keyword capture-modal-id)))]
    (when (and displayable? capture-modal-id variant)
      (when-not (= "off" variant)
        (merge {:id                                   "email-capture"
                :capture-modal-id                     capture-modal-id
                :email-capture/variant                variant
                :email-capture.dismiss/target         [e/biz|email-capture|dismissed {:id      capture-modal-id
                                                                                      :variant variant}]
                :email-capture.submit/target          [e/biz|email-capture|captured {:id      capture-modal-id
                                                                                     :variant variant
                                                                                     :email   email}]
                :email-capture.cta/id                 "email-capture-submit"
                :email-capture.text-field/id          "email-capture-input"
                :email-capture.text-field/placeholder "Enter Email Address"
                :email-capture.text-field/focused     focused
                :email-capture.text-field/keypath     textfield-keypath
                :email-capture.text-field/errors      errors
                :email-capture.text-field/email       email}
               (case [capture-modal-id variant]

                 ["first-pageview-email-capture" "2022-05"]
                 {:email-capture.photo/uuid-mob "fac95ac5-882c-4df0-9a1d-5edb4fd426fb"
                  :email-capture.photo/uuid-dsk "fac95ac5-882c-4df0-9a1d-5edb4fd426fb"
                  :email-capture.title/primary  [:div
                                                 [:div.title-2.proxima.shout "UNLOCK"]
                                                 [:div.title-1.canela.p-color "$50 Off"]
                                                 [:div.title-2.proxima
                                                  "Plus get exclusive offers, updates on new products, and more."]]
                  :email-capture.cta/value      "Sign me up"}

                 ["adv-quiz-email-capture" "2022-05"]
                 {:email-capture.photo/uuid-mob "05b10a97-3998-424d-b157-2270599b7971"
                  :email-capture.photo/uuid-dsk "05b10a97-3998-424d-b157-2270599b7971"
                  :email-capture.title/primary  [:div
                                                 [:div.title-2.proxima.shout "UNLOCK"]
                                                 [:div.title-1.canela.p-color "$50 Off"]
                                                 [:div.title-2.proxima
                                                  "Plus get exclusive offers, updates on new products, and more."]]

                  :email-capture.cta/value "Sign Up"}

                 ["first-pageview-email-capture" "2022-05b"]
                 {:email-capture.photo/uuid-mob "44052f45-4252-44f4-a47a-c0a94e0020a3"
                  :email-capture.photo/uuid-dsk "44052f45-4252-44f4-a47a-c0a94e0020a3"
                  :email-capture.title/primary  [:div
                                                 [:div.title-2.proxima
                                                  "Plus access to hair education, trending looks and more."]]
                  :email-capture.cta/value      "Sign me up"}

                 ["adv-quiz-email-capture" "2022-05b"]
                 {:email-capture.photo/uuid-mob "e7fdb284-4d9d-49ae-a119-da1858fa6fdd"
                  :email-capture.photo/uuid-dsk "e7fdb284-4d9d-49ae-a119-da1858fa6fdd"
                  :email-capture.title/primary  [:div
                                                 [:div.title-2.proxima
                                                  "Plus top stylist recommendations, trending looks and more."]]

                  :email-capture.cta/value "Sign Up"}

                 ;; ELSE
                 nil))))))

(c/defdynamic-component contentful-driven-template
  (did-mount
   [this]
   (let [{:as data
          :email-capture/keys [trigger-id variation-description template-content-id]} (c/get-props this)]
     (scroll/disable-body-scrolling)
     (publish e/control-menu-collapse-all)
     (publish e/biz|email-capture|deployed {:trigger-id            trigger-id
                                            :variation-description variation-description
                                            :template-content-id   template-content-id})))
  (will-unmount
   [this]
   (scroll/enable-body-scrolling))
  (render
   [this]
   (let [{:keys               [id]
          :email-capture/keys [content-type]
          :as                 data} (c/get-props this)
         template                   (case content-type
                                      "emailModalTemplate" email-capture-modal-template-1
                                      nil)]
     (if template
       (ui/modal
        {:close-attrs (apply utils/fake-href (:email-capture.dismiss/target data))
         :col-class   "col-12 col-5-on-tb col-4-on-dt flex justify-center"
         :bg-class    "bg-darken-4"}
        (template data))
       (js/console.error (str "Content-type not found: " content-type))))))

(defn matcher-matches? [app-state matcher]
  (case (:content/type matcher)
    "matchesAny"  (->> matcher
                       :must-satisfy-any-match
                       (map (partial matcher-matches? app-state))
                       (some true?))
    "matchesAll"  (->> matcher
                       :must-satisfy-all-matches
                       (map (partial matcher-matches? app-state))
                       (every? true?))
    "matchesNot"  (->> matcher
                       :must-not-satisfy
                       (matcher-matches? app-state)
                       not)
    "matchesPath" (let [cur-path (-> app-state
                                     (get-in k/navigation-uri)
                                     :path)]
                    (case (:path-matches matcher)
                      "starts with"         (clojure.string/starts-with? cur-path (:path matcher))
                      "contains"            (clojure.string/includes? cur-path (:path matcher))
                      "exactly matches"     (= cur-path (:path matcher))
                      "does not start with" (not (clojure.string/starts-with? cur-path (:path matcher)))))
    (js/console.error (str "No matching content/type for matcher " (pr-str matcher)))))
;; TODO: there are other path mathers not accounted for yet.

(defn contentful-driven-query [app-state]
  (let [cms-modal-data          (get-in app-state k/cms-email-modal)
        long-timer-started      (get-in app-state concept/long-timer-started-keypath)
        short-timer-starteds    (get-in app-state concept/short-timer-starteds-keypath)
        chosen-modal            (->> cms-modal-data
                                   vals
                                   (filter #(matcher-matches? app-state (-> % :email-modal-trigger :matcher)))
                                   first)
        errors                  (get-in app-state (conj k/field-errors ["email"]))
        focused                 (get-in app-state k/ui-focus)
        textfield-keypath       concept/textfield-keypath
        email                   (get-in app-state textfield-keypath)
        content                 (:email-modal-template chosen-modal)
        trigger-id              (-> chosen-modal :email-modal-trigger :trigger-id)
        template-content-id     (:template-content-id content)
        in-no-modal-experiment? (experiments/quiz-results-email-send-look? app-state)
        variation-description   (-> chosen-modal :description)]
    (when (and trigger-id
               template-content-id
               (not long-timer-started)
               (->> trigger-id (get short-timer-starteds) not))
      (when-not (and in-no-modal-experiment?
                     (= trigger-id "adv-quiz-email-capture"))
        {:id                                    "email-capture"
         :email-capture/trigger-id              trigger-id
         :email-capture/variation-description   variation-description
         :email-capture/template-content-id     template-content-id
         :email-capture/content-type            (:content/type content)
         :email-capture.dismiss/target          [e/biz|email-capture|dismissed {:trigger-id            trigger-id
                                                                                :variation-description variation-description
                                                                                :template-content-id   template-content-id}]
         :email-capture.submit/target           [e/biz|email-capture|captured {:trigger-id            trigger-id
                                                                               :variation-description variation-description
                                                                               :template-content-id   template-content-id
                                                                               :email                 email}]
         :email-capture.design/background-color (:background-color content)
         :email-capture.design/close-x-color    (:close-xcolor content)
         :email-capture.copy/title              (:title content)
         :email-capture.copy/subtitle           (:subtitle content)
         :email-capture.copy/supertitle         (:supertitle content)
         :email-capture.copy/fine-print-lead-in (:fine-print-lead-in content)
         :email-capture.cta/id                  "email-capture-submit"
         :email-capture.cta/value               (:cta-copy content)
         :email-capture.text-field/id           "email-capture-input"
         :email-capture.text-field/placeholder  (:email-input-field-placeholder-copy content)
         :email-capture.text-field/focused      focused
         :email-capture.text-field/keypath      textfield-keypath
         :email-capture.text-field/errors       errors
         :email-capture.text-field/email        email
         :email-capture.photo/url               (-> content :hero-image :file :url)
         :email-capture.photo/title             (-> content :hero-image :title)
         :email-capture.photo/description       (-> content :hero-image :description)}))))


(defn ^:export built-component [app-state opts]
  (if (-> app-state (get-in k/features) :contentful-driven-email-capture-modal)
    (when-let [data (contentful-driven-query app-state)]
      (c/build contentful-driven-template data opts))
    (when-let [data (query app-state)]
      (c/build template data opts))))
