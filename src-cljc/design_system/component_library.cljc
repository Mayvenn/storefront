(ns design-system.component-library
  (:require [storefront.component :as c]
            [storefront.components.ui :as ui]
            [storefront.keypaths :as keypaths]
            storefront.components.accordion
            storefront.components.footer-minimal
            storefront.components.flash
            storefront.components.sign-in
            storefront.components.video
            [storefront.platform.component-utils :as util]
            [storefront.events :as e]
            [storefront.transitions :as transitions]
            clojure.edn
            clojure.pprint))

(def components-list
  [{:title          "Accordion"
    :id             "accordion"
    :default-params {:expanded-indices #{1 2}
                     :sections         [{:title   "To show spacing"
                                         :content [{:paragraph [{:text "Stuff"}]}]}
                                        {:title   "example!"
                                         :content [{:paragraph [{:text "Yarr, Content!"}]}]}
                                        {:title   "Also open!"
                                         :content [{:paragraph [{:text "Don’t worry, "}
                                                                {:text "measuring your head for a wig",
                                                                 :url  "https://shop.mayvenn.com/blog/hair/how-to-measure-head-for-wig-size/"}
                                                                {:text " isn’t as complicated as it seems. Check out our easy to follow instructions here."}]}
                                                   {:paragraph [{:text "Paragraph 2"}]}]}
                                        {:title   "This one is closed, though"
                                         :content [{:paragraph [{:text "This is invisible."}]}]}
                                        {:title   "A Fourth"
                                         :content [{:paragraph [{:text "Stuff"}]}]}]}
    :component      storefront.components.accordion/component}
   {:title          "Minimal Footer"
    :id             "minimal-footer"
    :default-params {:call-number "510-867-5309"}
    :component      storefront.components.footer-minimal/component}
   {:title          "Flash"
    :id             "flash"
    :default-params {:success "It worked!"
                     :failure "It didn't work..."
                     :errors  {:error-message "Things went horribly wrong."}}
    :component      storefront.components.flash/component}
   {:title          "Sign In"
    :id             "sign-in"
    :default-params {:email          "acceptance+jasmine@mayvenn.com"
                     :password       "password"
                     :show-password? false
                     :field-errors   {["password"] [{:long-message "Something terrible happened!"}]}}
    :component      storefront.components.sign-in/component}
   {:title          "Video"
    :id             "video"
    :default-params {:youtube-id "dQw4w9WgXcQ"}
    :component      storefront.components.video/component}])

(defn ^:private component-id->component [component-id]
  (->> components-list
       (filter #(= component-id (:id %)))
       first))

(def ^:private checkered-background
  {:background-color    "#ffffff"
   :background-image    "repeating-linear-gradient(45deg, #4427c111 25%, transparent 25%, transparent 75%, #4427c111 75%, #4427c111), repeating-linear-gradient(45deg, #4427c111 25%, #ffffff 25%, #ffffff 75%, #4427c111 75%, #4427c111)"
   :background-position "0 0, 6px 6px"
   :background-size     "12px 12px"})

(c/defcomponent component
  [{:keys [current-component-id component-params]} _owner _opts]
  [:div.grid
   {:style {:grid-template-columns "150px auto"
            :grid-template-rows    "1fr 1fr"
            :min-height            "90vh"}}
   [:ul.p0
    {:style {:grid-row "1 / 3"}}
    (map (fn [{:keys [id title]}]
           [:li
            (merge {:key id}
                   (when (= id current-component-id)
                     {:style checkered-background
                      :class "bold"}))
            [:a.block.inherit-color.p2
             (util/route-to e/navigate-design-system-component-library {:query-params {:id id}})
             title]]) components-list)]
   [:div.p4
    {:style checkered-background}
    [:div.border-dotted.border-p-color
     (if component-params
       (-> current-component-id
           component-id->component
           :component
           (c/build component-params))
       [:div.error "Invalid user params!"])]]
   (ui/textarea {:id      "user-param-textarea"
                 :keypath keypaths/ui-component-library-user-params
                 :value   (with-out-str (clojure.pprint/pprint component-params))})])

(defn built-component
  [data _opts]
  (c/build component {:current-component-id (:id (get-in data keypaths/navigation-query-params))
                      :component-params (-> (get-in data keypaths/ui-component-library-user-params)
                                       clojure.edn/read-string
                                       (try (catch #?(:cljs :default :clj Throwable) _ nil)))} nil))

(defmethod transitions/transition-state e/navigate-design-system-component-library
  [_ event {:keys [query-params]} app-state]
  (->> query-params
       :id
       component-id->component
       :default-params
       str
       (assoc-in app-state keypaths/ui-component-library-user-params)))
