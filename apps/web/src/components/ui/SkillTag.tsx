interface SkillTagProps {
  name: string;
  onClick?: () => void;
}

export default function SkillTag({ name, onClick }: SkillTagProps) {
  return (
    <span
      onClick={onClick}
      className={`inline-block px-3 py-1 rounded-full text-sm border border-primary-200 text-primary-700 bg-primary-50 ${
        onClick ? 'cursor-pointer hover:bg-primary-100' : ''
      }`}
    >
      {name}
    </span>
  );
}
