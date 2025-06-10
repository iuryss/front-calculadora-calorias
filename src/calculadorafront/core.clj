(ns calculadorafront.core
  (:require [clojure.string :as str]
            [clj-http.client :as client]
            [cheshire.core :as json])
  (:import [java.net URLEncoder]))

(def api-url "http://localhost:3000")


(defn ler-inteiro [prompt]
  (print (str prompt ": "))
  (flush)
  (Integer/parseInt (read-line)))

(defn ler-string [prompt]
  (print (str prompt ": "))
  (flush)
  (read-line))

(defn mostrar-exercicio [id exercicio]
  (println (str  id ". Descricao: " (:name exercicio) "| Calorias por hora: " (:calories_per_hour exercicio))))

(defn mostrar-transacoes [id transacao]
  (println (str id ". Tipo: " (:tipo transacao) " | Descricao: " (:descricao transacao) "| Data: " (:data transacao) " | Quantidade: " (:quantidade transacao) "| Calorias: " (:valor transacao))))


(defn encode-url [s]
  (URLEncoder/encode s "UTF-8"))


(defn chamar-get [endpoint]
  (let [resposta (client/get (str api-url endpoint) {:accept :json})
        corpo (json/parse-string (:body resposta) true)]
    (if (= 200 (:status resposta))
      corpo
      (do
        (println "Erro ao chamar API:" (:status resposta))
        nil))))

(defn registrar-usuario []
  (println "\n=== REGISTRO DE USUARIO ===")
  (let [nome (ler-string "Digite seu Nome")
        sexo (ler-string "Digite seu Sexo (M/F)")
        peso (ler-inteiro "Digite seu Peso (kg)")
        altura (ler-inteiro "Digite sua Altura (cm)")
        idade (ler-inteiro "Digite sua Idade")]
    (client/post (str api-url "/registrar-usuario")
                 {:body (json/generate-string {:nome nome
                                               :peso peso
                                               :altura altura
                                               :sexo sexo
                                               :idade idade})
                  :headers {"Content-Type" "application/json"}})
    (println "\nUsuário cadastrado!")))


(defn menu []
  (println "\n=== CALCULADORA DE CALORIAS ===")
  (println "1. Registrar Consumo de Alimento (Ganho)")
  (println "2. Registrar Atividade Fisica (Perda)")
  (println "3. Consultar Extrato de Transacoes")
  (println "4. Consultar Saldo de Calorias")
  (println "5. Limpar Registros")
  (println "0. Sair")
  (let [opcao (ler-inteiro "Escolha uma opcao")]
    (case opcao
      1 (do
          (let [descricao (ler-string "Descrição do alimento")
                data (ler-string "Data (dd/MM/yyyy)")
                quantidade (ler-inteiro "Quantidade (gramas)")
                alimentos (chamar-get (str "/alimentos?descricao=" descricao))]
                (println alimentos)
            ;;     (client/post (str api-url "/registrar")
            ;;              {:body (json/generate-string {:tipo "ganho"
            ;;                                            :descricao descricao
            ;;                                            :data data
            ;;                                            :quantidade quantidade})
            ;;               :headers {"Content-Type" "application/json"}})
            ;; (println "\nConsumo registrado!")
            ))
      2 (do
          (let [descricao (ler-string "Descricao do exercicio")
                data (ler-string "Data (dd/MM/yyyy)")
                quantidade (ler-inteiro "Tempo (minutos)")
                resposta (chamar-get (str "/exercicios?descricao=" descricao))
                exercicios (:exercicios resposta)]
            (println "Escolha o exercicio:")
            (doall (map mostrar-exercicio (range 1 6) (:exercicios exercicios)))
                (let [index (ler-inteiro "Escolha o numero do exercicio:")]
                  (client/post (str api-url "/registrar")
                         {:body (json/generate-string {:tipo "perda"
                                                       :descricao descricao
                                                       :data data
                                                       :quantidade quantidade
                                                       :index (dec index)})
                          :headers {"Content-Type" "application/json"}})
                  (println "\nAtividade registrada!")
            )))
      3 (do
          (let [inicio (ler-string "Data Início (dd/MM/yyyy)")
                fim (ler-string "Data Fim (dd/MM/yyyy)")
                inicio-enc (encode-url inicio)
                fim-enc    (encode-url fim)
                resposta (chamar-get (str "/registro-do-periodo?inicio=" inicio-enc "&fim=" fim-enc))]
                (println "\nExtrato de Transações:")
                (doall (map mostrar-transacoes (range 1 (inc (count (:registros resposta)))) (:registros resposta)))))
      4 (do
          (let [inicio (ler-string "Data Início (dd/MM/yyyy)")
                fim (ler-string "Data Fim (dd/MM/yyyy)")
                inicio-enc (encode-url inicio)
                fim-enc    (encode-url fim)
                resposta (chamar-get (str "/saldo-do-periodo?inicio=" inicio-enc "&fim=" fim-enc))]
                (println "\nSaldo de Transações:" (:saldo resposta) )))
      5 (do
          (client/delete (str api-url "/limpar"))
          (println "\nRegistros apagados!"))
      0 (do
          (println "\nSaindo... Ate a proxima!")
          (System/exit 0))
      (println "\nOpcao invalida!")))
  (recur))

(defn -main [& _]
  (registrar-usuario)
  (menu))
