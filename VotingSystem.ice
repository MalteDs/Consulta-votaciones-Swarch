module VotingSystem {
    struct VotingTable {
        string location;    // Dirección del lugar de votación
        string city;        // Ciudad donde se encuentra la mesa
        string department;  // Departamento de la mesa
        string votingPlace; // Nombre del puesto de votación
    }


    struct VoterInfo {
        string documentId;        // Documento de identidad del votante
        VotingTable table;        // Información de la mesa asignada
        bool isPrimeFactorsPrime; // Indica si el número de factores primos es primo (1: sí, 0: no)
        float responseTime;       // Tiempo en segundos que tomó la consulta
        string firstName;         // Nombre del votante
        string lastName;          // Apellido del votante
    }


    exception VotingSystemException {
        string message;         // Mensaje descriptivo del error
    }

    sequence<string> StringSeq;
    sequence<VoterInfo> VoterInfoSeq;

    interface VotingService {
        void registerClient(string clientId)
            throws VotingSystemException;

        void unregisterClient(string clientId)
            throws VotingSystemException;

        VoterInfo getVotingInfo(string documentId)
            throws VotingSystemException;

        VoterInfoSeq performBatchQuery(string clientId, StringSeq documentIds)
            throws VotingSystemException;

        void notifyObservers(string message)
            throws VotingSystemException;

        StringSeq getServerLog()
            throws VotingSystemException;

        void distributeTasks(StringSeq documentIds)
            throws VotingSystemException;
    }

    interface Observer {
        void update(string message);
    }
}