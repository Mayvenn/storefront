(ns stylist-directory.stylists
  (:require [stylist-directory.keypaths :as keypaths]))

(defn stylist-by-id
  [app-state stylist-id]
  (->> (conj keypaths/stylists stylist-id)
       (get-in app-state)))

(defn ->display-name
  [{:keys [store-nickname address] :as stylist}]
  (when stylist
    (if (= store-nickname (:firstname address))
      (str (:firstname address) " " (:lastname address))
      store-nickname)))
