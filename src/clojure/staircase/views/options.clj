(ns staircase.views.options
  (:use staircase.views.services)
  (:require staircase.views.modals
            [hiccup.def :refer (defelem)]))

(defn user-options-dialogue [_]
  (staircase.views.modals/template "Options"
                                   [:tabset.options

                                    [:tab
                                     [:tab-heading "Active services"]
                                     [:div.services
                                      [:table.table
                                       [:tbody (service-row {:ng-repeat "service in services"}) ]]
                                      (add-service-controls)]]

                                    [:tab
                                     [:tab-heading "Starting Tools"]
                                     [:div.starting-tools
                                      [:table.table
                                       [:tbody
                                        [:tr {:ng-repeat "tool in startingPoints"}
                                         [:td [:input {:type "checkbox" :ng-model "tool.active"}]]
                                         [:td "{{tool.ident}}"]
                                         [:td "{{tool.heading}}"]
                                         [:td [:code "{{tool.args | json }}"]]
                                         ]]]]]

                                    ]))

(defn edit-step-dialogue [config]
  (staircase.views.modals/template "Edit step data"
                                   [:form.form
                                    [:editable-data {:data "data"}]]))

(defn choose-tool-dialogue [config]
  (staircase.views.modals/template "Choose tool"
                                   [:ul.list-group
                                    [:li.list-group-item
                                     {:ng-repeat "item in items"
                                      :ng-class "{active: item == selected.item}"}
                                     [:a {:ng-click "selected.item = item"} "{{item.heading}}"]]]))
