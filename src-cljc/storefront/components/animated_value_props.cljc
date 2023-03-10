(ns storefront.components.animated-value-props
  (:require [storefront.component :as component]
            [storefront.components.ui :as ui]
            [storefront.effects :as effects]
            [storefront.events :as events]
            [storefront.transitions :as transitions]
            [storefront.components.svg :as svg]
            [storefront.platform.messages :as messages]))

(def selection-timer (atom nil))

(defn set-target [this index]
  #?@(:cljs [(js/clearTimeout @selection-timer)
             (component/set-state! this :target index)
             (reset! selection-timer
                     (js/setTimeout
                      (partial set-target this (->> (range 3)
                                                    cycle
                                                    (drop (inc index))
                                                    first))
                      6000))]))


(def props
  [{:index 0
    :image "https://ucarecdn.com/a9499cf5-aad2-4429-85ae-a985b04dd09f/"
    :icon  :svg/calendar
    :copy  "Find your dream look with quality-guaranteed hair."}
   {:index 1
    :image "https://ucarecdn.com/28628eaf-d8fd-43d9-a1c0-a7e0fca7699e/"
    :icon  :svg/calendar
    :copy  "Endless possibilities with wig customization services"}
   {:index 2
    :image "https://ucarecdn.com/1ac41bac-daa7-4873-b54d-0f865295bf95/"
    :icon  :svg/calendar
    :copy  "Our licensed stylists provide expertise you can trust."}])

(component/defdynamic-component component
  (render [this]
          (component/html
           (let [current-target (:target (component/get-state this))]
             [:div.grid.columns-1.columns-2-on-tb-dt
              (ui/img {:src   (->> props
                                  (filter #(= current-target (:index %)))
                                  first
                                  :image)
                       :alt   ""
                       :class "col-12"})
              [:div.flex.flex-column.gap-8
               {:style {:padding "1.5rem"}}
               [:div.title-1.canela "Finding your perfect look has never been easier"]
               [:div.flex.flex-column.gap-2
                (for [{:keys [index icon copy]} props
                      :let [targeted? (= index current-target)]]

                  [:a.flex.gap-2.items-center.pointer
                   (merge
                    {:key index
                     :on-click #?(:cljs (fn [e]
                                          (.stopPropagation e)
                                          (set-target this index))
                                  :clj nil)}
                    (when-not targeted?
                      {:style {:color "#767377"}}))
                   (svg/symbolic->html [icon {:width "48px"
                                              :height "48px"}])
                   copy])]]])))
  (did-mount
   [this]
   (set-target this 0)))
