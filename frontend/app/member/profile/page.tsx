"use client";
import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { getMemberSession } from "@/lib/auth";

export default function MemberProfilePage() {
  const [member, setMember] = useState<any>(null);
  const router = useRouter();

  useEffect(() => {
    const memberSession = getMemberSession();
    if (memberSession?.sessionToken) {
      setMember(memberSession);
    } else {
      router.push("/login");
    }
  }, [router]);

  return (
    <div className="mx-auto max-w-2xl px-4 md:px-6 py-8 md:py-16">
      <div className="eyebrow">Member</div>
      <h1 className="h1 mt-2">Your Profile</h1>
      {member && (
        <div className="card p-5 md:p-8 mt-6 md:mt-8 space-y-3 md:space-y-4">
          <Row k="Member ID" v={member.memberId || "—"} />
          <Row k="Member Type" v={member.memberType ? member.memberType.replace("_", " ") : "—"} />
          <Row k="Identifier" v={member.maskedIdentifier || "—"} />
          <div className="pt-2">
            <Link href="/member/bookings" className="btn-primary inline-flex">View My Bookings</Link>
          </div>
        </div>
      )}
    </div>
  );
}
function Row({ k, v }: { k: string; v: string }) {
  return (
    <div className="flex justify-between border-b border-navy/10 pb-3 last:border-0 gap-4">
      <span className="text-navy/60 text-xs md:text-sm">{k}</span>
      <span className="text-navy font-medium text-sm md:text-base text-right">{v}</span>
    </div>
  );
}
