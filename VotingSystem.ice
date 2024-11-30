module Consulta {
    module votaciones {

        // Estructura para representar la asignación de mesas de votación
        struct VotingTable {
            int tableId;            // ID único de la mesa de votación
            string location;        // Dirección del lugar de votación
            string city;            // Ciudad donde se encuentra la mesa
        }

        // Estructura para representar los detalles de un votante
        struct VoterInfo {
            string documentId;      // Documento de identidad del votante
            VotingTable table;      // Información de la mesa asignada
            bool isPrimeFactorsPrime; // Indica si el número de factores primos es primo (1: sí, 0: no)
            float responseTime;     // Tiempo en segundos que tomó la consulta
        }

        // Excepción para errores relacionados con el sistema
        exception VotingSystemException {
            string message;         // Mensaje descriptivo del error
        }

        // Declaración de secuencias
        sequence<string> StringSeq;
        sequence<VoterInfo> VoterInfoSeq;

        // Interfaz para el servicio de votaciones
        interface VotingSystem {

            // Registra un cliente como observador
            void registerClient(string clientId)
                throws VotingSystemException;

            // Remueve un cliente registrado
            void unregisterClient(string clientId)
                throws VotingSystemException;

            // Consulta información de votación por documento de identidad
            VoterInfo getVotingInfo(string documentId)
                throws VotingSystemException;

            // Realiza múltiples consultas concurrentes desde un cliente
            VoterInfoSeq performBatchQuery(string clientId, StringSeq documentIds)
                throws VotingSystemException;

            // Notifica a los clientes registrados con datos de consulta
            void notifyObservers(string message)
                throws VotingSystemException;

            // Obtiene un log resumido del servidor
            StringSeq getServerLog()
                throws VotingSystemException;
        }
    }
}
