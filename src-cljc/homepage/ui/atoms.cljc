(ns homepage.ui.atoms)

;; TODO can this use ucare-img utilities / picture tag?
(defn divider-atom
  [ucare-id]
  (let [uri (str "url('//ucarecdn.com/" ucare-id "/-/resize/x24/')")]
    [:div {:style {:background-image    uri
                   :background-position "center"
                   :background-repeat   "repeat-x"
                   :height              "24px"}}]))

(def horizontal-rule-atom
  [:div.border-bottom.border-width-1
   {:style {:border-color "#EEEEEE"}}])

