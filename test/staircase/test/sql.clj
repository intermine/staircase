(ns staircase.test.sql
  (:import java.sql.SQLException)
  (:use clojure.test
        staircase.helpers
        staircase.sql
        [clojure.tools.logging :only (info warn debug)]
        [staircase.config :only (db-options)]
        [com.stuartsierra.component :as component]
        [environ.core :only (env)])
  (:require [clojure.java.jdbc :as sql]))

(def db-spec (db-options env))

(def test-rows (vec (for [value ["foo" "bar" "baz" "quux"]] [(new-id) value])))

(defn ignore-errors [f]
  (try (f) (catch SQLException e nil)))

(defn drop-tables [] 
  (debug "Dropping tables...")
  (doseq [table [:entities :owned_entities :table_a :table_b]]
    (ignore-errors #(sql/db-do-commands db-spec (str "TRUNCATE " (name table)) (sql/drop-table-ddl table)))
    (debug "Dropped" table)))

(defn build-tables []
  (sql/db-do-commands db-spec
                      (sql/create-table-ddl :entities [:id :uuid] [:value :text])
                      (sql/create-table-ddl :owned_entities [:id :uuid] [:owner "varchar(1024)" "NOT NULL"] [:value :text])))

(defn clean-slate [f]
  (drop-tables)
  (f))

(defn clean-up [f]
  (try (f) (finally (drop-tables))))

(defn insert-data [f]
  (apply sql/insert! db-spec :entities [:id :value] test-rows)
  (apply sql/insert! db-spec :owned_entities [:id :value :owner] (map #(conj % "foo@bar.org") test-rows))
  (f))

(defn set-up [f]
  (build-tables)
  (f))

(use-fixtures :each clean-slate set-up insert-data clean-up)

(deftest ^:database test-get-table-names
  (let [current-table (get-table-names db-spec)]
    (testing "No false negatives"
      (is (current-table "entities")))
    (testing "No false positives"
      (is (not (current-table "foo"))))))

(deftest ^:database test-count-where
  (testing "No false negatives"
    (dorun (for [[_ value] test-rows]
      (is (= 1 (count-where db-spec :entities [:= :value value])) (str value " should be in the db")))))
  (testing "No false positives"
    (dorun (for [n (range 100)]
      (is (= 0 (count-where db-spec :entities [:= :value (str n)])) (str "There is no row with the value: " n))))))

(deftest ^:database test-exists
  (testing "No false negatives"
    (dorun (for [[id value] test-rows]
      (is (exists db-spec :entities id) (str value " should be in the db")))))
  (testing "No false positives"
    (dorun (for [_ (range 100)]
      (is (not (exists db-spec :entities (new-id))))))))

(deftest ^:database test-update-entity
  (let [quux-id (get-in test-rows [3 0])
        updated (update-entity db-spec :entities quux-id {:value "now even quuxier"})]
    (testing "Return value"
      (is (= "now even quuxier" (:value updated))))
    (testing "There exists an entity of that id"
      (is (exists db-spec :entities quux-id)))
    (testing "There is still only one of that id"
      (is (= 1 (count-where db-spec :entities [:= :id quux-id]))))
    (testing "There are no rows with the old value"
      (is (= 0 (count-where db-spec :entities [:= :value "quux"]))))))

(deftest ^:database test-update-owned-entity
  (let [quux-id (get-in test-rows [3 0])
        updated (update-owned-entity db-spec
                                     :owned_entities
                                     {:id quux-id :user "foo@bar.org"}
                                     {:value "now even quuxier"})]
    (testing "Return value"
      (is (= "now even quuxier" (:value updated))))
    (testing "There exists an entity of that id"
      (is (exists db-spec :owned_entities quux-id)))
    (testing "There is still only one of that id"
      (is (= 1 (count-where db-spec :owned_entities [:= :id quux-id]))))
    (testing "There are no rows with the old value"
      (is (= 0 (count-where db-spec :owned_entities [:= :value "quux"]))))))

(deftest ^:database test-update-owned-entity-wrong-owner
  (let [quux-id (get-in test-rows [3 0])
        updated (update-owned-entity db-spec
                                     :owned_entities
                                     {:id quux-id :user "bar@foo.org"}
                                     {:value "now even quuxier"})]
    (testing "Return value"
      (is (= nil updated)))
    (testing "There still exists an entity of that id"
      (is (exists db-spec :owned_entities quux-id)))
    (testing "There is still only one of that id"
      (is (= 1 (count-where db-spec :owned_entities [:= :id quux-id]))))
    (testing "There are no rows with the new value"
      (is (= 0 (count-where db-spec :owned_entities [:= :value "now even quuxier"]))))))

(deftest ^:database test-update-entity-string-map
  (try
    (let [quux-id (get-in test-rows [3 0])
          updated (update-entity db-spec :entities quux-id {"value" "now even quuxier"})]
      (testing "Return value"
        (is (= "now even quuxier" (:value updated))))
      (testing "There exists an entity of that id"
        (is (exists db-spec :entities quux-id)))
      (testing "There is still only one of that id"
        (is (= 1 (count-where db-spec :entities [:= :id quux-id]))))
      (testing "There are no rows with the old value"
        (is (= 0 (count-where db-spec :entities [:= :value "quux"])))))
    (catch SQLException e
      (let [sw (java.io.StringWriter.)]
        (binding [*out* sw]
          (sql/print-sql-exception-chain e))
        (warn (str sw)))
      (throw e))))

(deftest ^:database test-update-safety
  (let [quux-id (get-in test-rows [3 0])
        new-id  (new-id)
        updated (update-entity db-spec :entities (str quux-id) {:id new-id :value "now even quuxier"})]
    (testing "Return value"
      (is (= "now even quuxier" (:value updated)))
      (is (= quux-id (:id updated))))
    (testing "There exists an entity of that id"
      (is (exists db-spec :entities quux-id)))
    (testing "There is still only one of that id"
      (is (= 1 (count-where db-spec :entities [:= :id quux-id]))))
    (testing "There are no rows with the old value"
      (is (= 0 (count-where db-spec :entities [:= :value "quux"]))))
    (testing "There are no rows with the new id"
      (is (= 0 (count-where db-spec :entities [:= :id new-id]))))))

