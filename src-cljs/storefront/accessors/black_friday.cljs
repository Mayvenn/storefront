(ns storefront.accessors.black-friday)

(defn after-black-friday-start? []
  (> (js/Date.now) (js/Date.parse "2015-11-27T05:00:00Z")))

(defn black-friday-sale? []
  (and (after-black-friday-start?) (< (js/Date.now) (js/Date.parse "2015-12-01T08:00:00Z"))))
