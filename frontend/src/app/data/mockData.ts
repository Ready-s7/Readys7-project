export interface Project {
  id: string;
  title: string;
  description: string;
  category: string;
  budget: string;
  duration: string;
  skills: string[];
  clientName: string;
  clientRating: number;
  proposals: number;
  postedDate: string;
  status: "open" | "in_progress" | "completed";
}

export interface Developer {
  id: string;
  name: string;
  title: string;
  rating: number;
  reviewCount: number;
  completedProjects: number;
  skills: string[];
  hourlyRate: string;
  responseTime: string;
  description: string;
  portfolio: string[];
  location: string;
  avatar: string;
}

export const categories = [
  { id: "web", label: "웹 개발", icon: "💻" },
  { id: "mobile", label: "앱 개발", icon: "📱" },
  { id: "ai", label: "AI/ML", icon: "🤖" },
  { id: "blockchain", label: "블록체인", icon: "⛓️" },
  { id: "game", label: "게임 개발", icon: "🎮" },
  { id: "design", label: "UI/UX", icon: "🎨" },
];

export const mockProjects: Project[] = [
  {
    id: "1",
    title: "쇼핑몰 웹사이트 개발",
    description: "반응형 쇼핑몰 웹사이트가 필요합니다. React와 Node.js를 사용하여 제작하고, 결제 시스템 연동이 필요합니다.",
    category: "web",
    budget: "300-500만원",
    duration: "2개월",
    skills: ["React", "Node.js", "MongoDB", "결제연동"],
    clientName: "김민수",
    clientRating: 4.8,
    proposals: 12,
    postedDate: "2026-04-05",
    status: "open",
  },
  {
    id: "2",
    title: "iOS/Android 모바일 앱 개발",
    description: "크로스 플랫폼 모바일 앱 개발이 필요합니다. Flutter 또는 React Native를 사용하여 제작해주세요.",
    category: "mobile",
    budget: "500-700만원",
    duration: "3개월",
    skills: ["Flutter", "React Native", "Firebase", "REST API"],
    clientName: "박지영",
    clientRating: 4.9,
    proposals: 8,
    postedDate: "2026-04-06",
    status: "open",
  },
  {
    id: "3",
    title: "AI 챗봇 개발",
    description: "고객 상담을 위한 AI 챗봇 개발 프로젝트입니다. GPT API를 활용하여 자연스러운 대화가 가능해야 합니다.",
    category: "ai",
    budget: "400-600만원",
    duration: "1.5개월",
    skills: ["Python", "OpenAI API", "NLP", "FastAPI"],
    clientName: "이준호",
    clientRating: 4.7,
    proposals: 15,
    postedDate: "2026-04-04",
    status: "open",
  },
  {
    id: "4",
    title: "NFT 마켓플레이스 구축",
    description: "블록체인 기반 NFT 거래 플랫폼이 필요합니다. 스마트 컨트랙트 개발 경험이 있으신 분을 찾습니다.",
    category: "blockchain",
    budget: "800-1000만원",
    duration: "4개월",
    skills: ["Solidity", "Web3.js", "Ethereum", "IPFS"],
    clientName: "최수진",
    clientRating: 5.0,
    proposals: 6,
    postedDate: "2026-04-07",
    status: "open",
  },
  {
    id: "5",
    title: "관리자 대시보드 개발",
    description: "데이터 시각화와 관리 기능이 포함된 대시보드가 필요합니다. 실시간 업데이트 기능이 필요합니다.",
    category: "web",
    budget: "200-300만원",
    duration: "1개월",
    skills: ["React", "TypeScript", "Chart.js", "WebSocket"],
    clientName: "강태훈",
    clientRating: 4.6,
    proposals: 10,
    postedDate: "2026-04-03",
    status: "in_progress",
  },
  {
    id: "6",
    title: "모바일 게임 개발",
    description: "캐주얼 퍼즐 게임 개발 프로젝트입니다. Unity를 사용하며, 광고 및 인앱 결제 연동이 필요합니다.",
    category: "game",
    budget: "600-800만원",
    duration: "3개월",
    skills: ["Unity", "C#", "Game Design", "AdMob"],
    clientName: "윤서연",
    clientRating: 4.8,
    proposals: 7,
    postedDate: "2026-04-02",
    status: "open",
  },
];

export const mockDevelopers: Developer[] = [
  {
    id: "1",
    name: "류호정",
    title: "풀스택 개발자",
    rating: 4.9,
    reviewCount: 48,
    completedProjects: 52,
    skills: ["React", "Node.js", "TypeScript", "AWS", "Docker"],
    hourlyRate: "5-7만원",
    responseTime: "1시간 이내",
    description: "10년 경력의 풀스택 개발자입니다. 대기업 프로젝트부터 스타트업까지 다양한 경험이 있습니다.",
    portfolio: [
      "https://images.unsplash.com/photo-1566915896913-549d796d2166?w=400",
      "https://images.unsplash.com/photo-1627599936744-51d288f89af4?w=400",
    ],
    location: "서울",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Felix",
  },
  {
    id: "2",
    name: "이석형",
    title: "모바일 앱 개발자",
    rating: 5.0,
    reviewCount: 35,
    completedProjects: 38,
    skills: ["Flutter", "React Native", "iOS", "Android", "Firebase"],
    hourlyRate: "6-8만원",
    responseTime: "30분 이내",
    description: "모바일 앱 전문 개발자로 iOS와 Android 네이티브 개발 경험이 풍부합니다.",
    portfolio: [
      "https://images.unsplash.com/photo-1633250391894-397930e3f5f2?w=400",
    ],
    location: "경기",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Aneka",
  },
  {
    id: "3",
    name: "박수지",
    title: "AI/ML 엔지니어",
    rating: 4.8,
    reviewCount: 28,
    completedProjects: 30,
    skills: ["Python", "TensorFlow", "PyTorch", "NLP", "Computer Vision"],
    hourlyRate: "7-9만원",
    responseTime: "2시간 이내",
    description: "AI와 머신러닝 분야 전문가입니다. 다양한 AI 프로젝트 경험이 있습니다.",
    portfolio: [
      "https://images.unsplash.com/photo-1566915896913-549d796d2166?w=400",
    ],
    location: "서울",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=John",
  },
  {
    id: "4",
    name: "정호진",
    title: "블록체인 개발자",
    rating: 4.9,
    reviewCount: 22,
    completedProjects: 25,
    skills: ["Solidity", "Web3.js", "Ethereum", "Smart Contract", "DeFi"],
    hourlyRate: "8-10만원",
    responseTime: "1시간 이내",
    description: "블록체인과 스마트 컨트랙트 전문 개발자입니다. DeFi 프로젝트 경험이 많습니다.",
    portfolio: [],
    location: "서울",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Sarah",
  },
  {
    id: "5",
    name: "최형민",
    title: "프론트엔드 개발자",
    rating: 4.7,
    reviewCount: 42,
    completedProjects: 45,
    skills: ["React", "Vue.js", "TypeScript", "Tailwind CSS", "Next.js"],
    hourlyRate: "4-6만원",
    responseTime: "3시간 이내",
    description: "사용자 경험을 최우선으로 생각하는 프론트엔드 개발자입니다.",
    portfolio: [
      "https://images.unsplash.com/photo-1627599936744-51d288f89af4?w=400",
    ],
    location: "부산",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Minho",
  },
  {
    id: "6",
    name: "안형욱",
    title: "게임 개발자",
    rating: 4.8,
    reviewCount: 18,
    completedProjects: 20,
    skills: ["Unity", "C#", "Unreal Engine", "Game Design", "3D Modeling"],
    hourlyRate: "5-7만원",
    responseTime: "2시간 이내",
    description: "모바일 및 PC 게임 개발 경험이 풍부한 게임 개발자입니다.",
    portfolio: [],
    location: "서울",
    avatar: "https://api.dicebear.com/7.x/avataaars/svg?seed=Yujin",
  },
];

export const reviews = [
  {
    id: "1",
    developerName: "김태현",
    clientName: "박철수",
    rating: 5,
    comment: "매우 전문적이고 빠른 작업으로 만족스러운 결과를 얻었습니다. 적극 추천합니다!",
    projectTitle: "쇼핑몰 웹사이트 구축",
    date: "2026-03-15",
  },
  {
    id: "2",
    developerName: "이지민",
    clientName: "최영희",
    rating: 5,
    comment: "앱 개발 전 과정을 꼼꼼하게 진행해주셨고, 커뮤니케이션도 원활했습니다.",
    projectTitle: "배달 앱 개발",
    date: "2026-03-20",
  },
  {
    id: "3",
    developerName: "박준서",
    clientName: "이동욱",
    rating: 5,
    comment: "AI 모델 성능이 기대 이상이었습니다. 다음 프로젝트도 함께하고 싶습니다.",
    projectTitle: "이미지 인식 AI 개발",
    date: "2026-03-10",
  },
];
