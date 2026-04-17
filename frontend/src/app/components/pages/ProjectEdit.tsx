import { useState, useEffect } from "react";
import { useParams, useNavigate } from "react-router";
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card";
import { Button } from "../ui/button";
import { Input } from "../ui/input";
import { Label } from "../ui/label";
import { Textarea } from "../ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../ui/select";
import { Badge } from "../ui/badge";
import { Loader2, ArrowLeft, X } from "lucide-react";
import { projectApi, categoryApi, skillApi } from "../../../api/apiService";
import type { CategoryDto } from "../../../api/types";
import { toast } from "sonner";
import { useAuth } from "../../../context/AuthContext";

export function ProjectEdit() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { userRole, isLoggedIn, userId } = useAuth();
  
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [allSkills, setAllSkills] = useState<string[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  const [formData, setFormData] = useState({
    title: "",
    description: "",
    categoryId: "",
    minBudget: "",
    maxBudget: "",
    duration: "",
    maxProposalCount: "10",
  });
  
  const [selectedSkills, setSelectedSkills] = useState<string[]>([]);

  useEffect(() => {
    if (!isLoggedIn) {
      toast.error("로그인이 필요합니다.");
      navigate("/login");
      return;
    }
    loadData();
  }, [id, isLoggedIn]);

  const loadData = async () => {
    setIsLoading(true);
    try {
      const [catRes, skillRes, projectRes] = await Promise.all([
        categoryApi.getAll(),
        skillApi.getAll(0, 100),
        projectApi.getById(Number(id))
      ]);
      
      // 카테고리 데이터 처리
      let catData = catRes.data.data;
      if (Array.isArray(catData) && catData.length === 2 && typeof catData[0] === 'string') {
        catData = catData[1] as any;
      }
      const categoriesArray = Array.isArray(catData) ? catData : [];
      setCategories(categoriesArray);

      // 스킬 데이터 처리
      const skillNames = skillRes.data.data.content.map((s: any) => s.name);
      setAllSkills(skillNames);

      // 프로젝트 데이터 처리
      const p = projectRes.data.data;
      
      // 권한 체크: 소유자나 관리자가 아니면 튕김
      if (userRole !== "ADMIN" && Number(p.clientUserId) !== Number(userId)) {
        toast.error("수정 권한이 없습니다.");
        navigate(`/projects/${id}`);
        return;
      }

      setFormData({
        title: p.title,
        description: p.description,
        categoryId: String(categoriesArray.find((c: any) => c.name === p.category)?.id || ""),
        minBudget: String(p.minBudget),
        maxBudget: String(p.maxBudget),
        duration: String(p.duration),
        maxProposalCount: String(p.maxProposalCount),
      });
      setSelectedSkills(p.skills || []);

    } catch (err) {
      toast.error("데이터를 불러오는데 실패했습니다.");
      navigate(-1);
    } finally {
      setIsLoading(false);
    }
  };

  const addSkill = (skill: string) => {
    if (skill && !selectedSkills.includes(skill)) {
      setSelectedSkills([...selectedSkills, skill]);
    }
  };

  const removeSkill = (s: string) => {
    setSelectedSkills(selectedSkills.filter(item => item !== s));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.categoryId) return toast.error("카테고리를 선택해주세요.");
    if (selectedSkills.length === 0) return toast.error("하나 이상의 기술 스택을 선택해주세요.");
    
    setIsSubmitting(true);
    try {
      await projectApi.update(Number(id), {
        title: formData.title,
        description: formData.description,
        categoryId: Number(formData.categoryId),
        minBudget: Number(formData.minBudget),
        maxBudget: Number(formData.maxBudget),
        duration: Number(formData.duration),
        skills: selectedSkills,
        maxProposalCount: Number(formData.maxProposalCount),
      });
      toast.success("프로젝트가 성공적으로 수정되었습니다.");
      navigate(`/projects/${id}`);
    } catch (err: any) {
      toast.error(err?.response?.data?.message || "수정 실패");
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isLoading) return <div className="flex justify-center py-32"><Loader2 className="animate-spin text-blue-600 w-10 h-10" /></div>;

  return (
    <div className="min-h-screen bg-gray-50 py-12">
      <div className="container mx-auto px-4 max-w-3xl">
        <Button variant="ghost" onClick={() => navigate(-1)} className="mb-6">
          <ArrowLeft className="w-4 h-4 mr-2" /> 뒤로가기
        </Button>
        
        <Card className="border-none shadow-sm">
          <CardHeader>
            <CardTitle className="text-2xl font-black text-gray-900">프로젝트 수정</CardTitle>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="space-y-2">
                <Label htmlFor="title">프로젝트 제목 *</Label>
                <Input 
                  id="title" 
                  value={formData.title} 
                  onChange={e => setFormData({...formData, title: e.target.value})} 
                  placeholder="예: 리액트 쇼핑몰 개발" 
                  required 
                />
              </div>

              <div className="space-y-2">
                <Label htmlFor="category">카테고리 *</Label>
                <Select value={formData.categoryId} onValueChange={v => setFormData({...formData, categoryId: v})}>
                  <SelectTrigger><SelectValue placeholder="카테고리 선택" /></SelectTrigger>
                  <SelectContent>
                    {categories.map(c => (
                      <SelectItem key={c.id} value={String(c.id)}>{c.icon} {c.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              <div className="space-y-2">
                <Label htmlFor="description">상세 내용 *</Label>
                <Textarea 
                  id="description" 
                  rows={10} 
                  value={formData.description} 
                  onChange={e => setFormData({...formData, description: e.target.value})} 
                  placeholder="프로젝트에 대한 상세 설명을 입력해주세요." 
                  required 
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="minBudget">최소 예산 (원) *</Label>
                  <Input 
                    id="minBudget" 
                    type="number" 
                    value={formData.minBudget} 
                    onChange={e => setFormData({...formData, minBudget: e.target.value})} 
                    required 
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="maxBudget">최대 예산 (원) *</Label>
                  <Input 
                    id="maxBudget" 
                    type="number" 
                    value={formData.maxBudget} 
                    onChange={e => setFormData({...formData, maxBudget: e.target.value})} 
                    required 
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="duration">예상 기간 (일) *</Label>
                  <Input 
                    id="duration" 
                    type="number" 
                    value={formData.duration} 
                    onChange={e => setFormData({...formData, duration: e.target.value})} 
                    required 
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="maxProposalCount">최대 제안수 *</Label>
                  <Input 
                    id="maxProposalCount" 
                    type="number" 
                    value={formData.maxProposalCount} 
                    onChange={e => setFormData({...formData, maxProposalCount: e.target.value})} 
                    required 
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label>필요 기술 스택 * (최소 1개)</Label>
                <Select onValueChange={addSkill}>
                  <SelectTrigger><SelectValue placeholder="목록에서 기술 선택" /></SelectTrigger>
                  <SelectContent>
                    {allSkills
                      .filter((skill) => !selectedSkills.includes(skill))
                      .map((skill) => (
                        <SelectItem key={skill} value={skill}>{skill}</SelectItem>
                      ))}
                  </SelectContent>
                </Select>
                {selectedSkills.length > 0 && (
                  <div className="flex flex-wrap gap-2 mt-3">
                    {selectedSkills.map(s => (
                      <Badge key={s} variant="secondary" className="pl-3 pr-1 py-1 flex items-center gap-1">
                        {s}
                        <button type="button" onClick={() => removeSkill(s)} className="hover:text-red-600 transition-colors">
                          <X className="w-3 h-3" />
                        </button>
                      </Badge>
                    ))}
                  </div>
                )}
              </div>

              <Button type="submit" className="w-full h-14 text-lg font-bold bg-blue-600 hover:bg-blue-700" disabled={isSubmitting}>
                {isSubmitting ? <Loader2 className="animate-spin mr-2" /> : null}
                수정 완료
              </Button>
            </form>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
