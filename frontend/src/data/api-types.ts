export interface ProcessedResults {
  parties: Party[];
  districts: District[];
  candidates: Candidate[];
}

export interface Party {
  name: string;
  code: string;
  mandates: number;
  votes: number;
}

export interface District {
  name: string;
  number: number;
  parties: Party[];
  voteStats: VoteStats;
  totalMandates: number;
}

export interface VoteStats {
  votesCounted: number;
  protocolsCounted: number;
  protocolsTotal: number;
  evotesCounted: boolean;
}

export interface Candidate {
  forename: string;
  surename: string;
  regNumber: number;
  votes: number;
  partyCode: string;
  districtNumber: number;
}

export type ElectionType = 'KOV2021' | 'RK2023' | 'KOV2025';
