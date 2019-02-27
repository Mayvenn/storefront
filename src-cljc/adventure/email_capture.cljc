(ns adventure.email-capture
  (:require
   #?@(:cljs [[om.core :as om]
              [storefront.platform.messages :as messages]
              [storefront.effects :as effects]
              [storefront.hooks.places-autocomplete :as places-autocomplete]
              [storefront.hooks.facebook-analytics :as facebook-analytics]
              [storefront.browser.cookie-jar :as cookie-jar]
              [storefront.history :as history]
              [storefront.hooks.stringer :as stringer]
              [sablono.core :as sablono]])
   [clojure.string :as string]
   [storefront.platform.component-utils :as utils]
   [adventure.components.header :as header]
   [adventure.keypaths :as keypaths]
   [storefront.keypaths :as storefront.keypaths]
   [storefront.component :as component]
   [storefront.events :as events]
   [storefront.components.ui :as ui]
   [storefront.transitions :as transitions]
   [storefront.effects :as effects]
   [storefront.trackings :as trackings]
   [adventure.progress :as progress]))

(defn ^:private query [data]
  (let [email  (get-in data storefront.keypaths/captured-email)
        errors (get-in data storefront.keypaths/field-errors)]
    {:email  email
     :errors errors}))

#?(:cljs
   (defn ^:private handle-on-change [^js/Event e]
     (messages/handle-message events/control-change-state
                              {:keypath storefront.keypaths/captured-email
                               :value   (.. e -target -value)})))

(defn ^:private valid-email? [email]
  (or (> 3 (count email))
      (not (string/includes? email "@"))))

(defn component
  [{:keys [email errors] :as thing-we-will-look-at} owner _]
  (component/create
   [:div.bg-lavender.white.center.flex.flex-auto.flex-column
    {:style {:background-image    (str "url(https://ucarecdn.com/03957478-feac-4e0c-aedf-e8e4a7123d69/)")
             :background-position "bottom"
             :background-repeat   "no-repeat"
             :background-size     "contain"}}
    [:div.flex.flex-column.items-center
     {:style {:height "246px"}}
     [:div.flex.items-center.center.mt3.pb3
      [:div.mr4
       (ui/ucare-img {:width "140"} "1970d88b-3798-4914-8a91-74288b09cc77")]]
     [:div.pt5
      [:div.h3.medium.mb2.col-8.mx-auto "Welcome! We can't wait for you to get a free install."]
      [:div.h5.light.mb2.col-8.mx-auto "Enter your e-mail to get started!"]
      [:div.col-12.mx-auto
       [:form.block.flex.justify-center
        [:input.h5.border-none.px3.bg-white.col-9
         (merge {:label       "e-mail address"
                 :data-test   "email-input"
                 :name        "email"
                 :id          "email-input"
                 :type        "email"
                 :autoFocus   true
                 :required    true
                 :placeholder "e-mail address"}
                #?(:cljs {:on-change (partial handle-on-change)}))]
        (let [disabled? (valid-email? email)]
          (ui/teal-button (merge {:style          {:width  "45px"
                                                   :height "45px"}
                                  :disabled?      disabled?
                                  :disabled-class "bg-light-gray gray"
                                  :data-test      "stylist-match-address-submit"
                                  :class          "flex items-center justify-center medium not-rounded x-group-item"}
                                 (utils/fake-href events/control-adventure-emailcapture-submit))
                          (ui/forward-arrow {:disabled? disabled?
                                             :width     "14"})))]]]]]))

(defn built-component [data opts]
  (component/build component (query data) opts))

(defn clear-field-errors [app-state]
  (assoc-in app-state storefront.keypaths/errors {}))

(defmethod transitions/transition-state events/control-adventure-emailcapture-submit [_ event args app-state]
  (let [email (get-in app-state storefront.keypaths/captured-email)]
    (if (valid-email? email)
      (assoc-in app-state storefront.keypaths/errors {:field-errors  {["email"] [{:path ["email"] :long-message "Email is invalid"}]}
                                                      :error-code    "invalid-input"
                                                      :error-message "Oops! Please fix the errors below."})
      (-> app-state
          clear-field-errors
          (assoc-in storefront.keypaths/popup nil)
          (assoc-in storefront.keypaths/email-capture-session "opted-in")))))

#?(:cljs
  (defmethod effects/perform-effects events/control-adventure-emailcapture-submit [_ _ args _ app-state]
    (when (empty? (get-in app-state storefront.keypaths/errors))
      (facebook-analytics/subscribe)
      (cookie-jar/save-email-capture-session (get-in app-state storefront.keypaths/cookie) "opted-in")
      (messages/handle-message events/navigate-adventure-time-frame))))
