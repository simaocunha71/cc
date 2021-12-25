public enum FileState {
    PREAUTICATION,
    SENDINGANSWER,
    SENDINGCODE,
    VALIDATECODE,

    PRESYNC,
    TESTTIME,
    
    SYNC,
    
    SENDINGAV,
    SENDINGFL,
    
    RECIVINGAV,
    RECIVINGFL,
    
    ERRORAV,
    ERRORFL,
    
    WAITOKAV,
    WAITOKFL,

    OKAV,
    OKFL;

    public static boolean isReceving (FileState s){
        return (s == RECIVINGAV || s == OKAV || s == RECIVINGFL || s == OKFL);
    }

    public static boolean interrupted(FileState state) {
        return (state != PREAUTICATION && state != SENDINGAV && state != SENDINGFL && state != ERRORAV && state != ERRORFL );
    }
}
