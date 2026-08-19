import { Injectable, Logger } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { VerificationCode } from '../../user/entities/user.entity';
import * as crypto from 'crypto';

interface CaptchaResult {
  id: string;
  svg: string;
  answer: string;
}

@Injectable()
export class CaptchaService {
  private readonly logger = new Logger(CaptchaService.name);

  constructor(
    @InjectRepository(VerificationCode)
    private verifyRepo: Repository<VerificationCode>,
  ) {}

  async generate(): Promise<CaptchaResult> {
    // 生成 4 位随机数字
    const answer = Math.floor(1000 + Math.random() * 9000).toString();
    const id = crypto.randomUUID();
    const expiresAt = new Date(Date.now() + 5 * 60 * 1000);

    // 存入数据库
    await this.verifyRepo.save({
      phone: 'captcha_' + id,
      code: answer,
      purpose: 'captcha',
      expiresAt,
    });

    return { id, answer, svg: this.generateSvg(answer) };
  }

  async verify(id: string, answer: string): Promise<boolean> {
    const record = await this.verifyRepo.findOne({
      where: { phone: 'captcha_' + id, purpose: 'captcha', isUsed: false },
      order: { createdAt: 'DESC' },
    });
    if (!record) return false;
    if (record.expiresAt < new Date()) return false;
    if (record.attemptCount >= 3) return false;

    record.attemptCount += 1;
    if (record.code === answer) {
      record.isUsed = true;
      await this.verifyRepo.save(record);
      return true;
    }
    await this.verifyRepo.save(record);
    return false;
  }

  private generateSvg(code: string): string {
    const colors = ['#FF6B6B', '#4ECDC4', '#45B7D1', '#96CEB4', '#FFEAA7', '#DDA0DD', '#98D8C8'];
    const chars = code.split('');
    let svg = '<svg xmlns="http://www.w3.org/2000/svg" width="160" height="60" viewBox="0 0 160 60">';
    svg += '<rect width="160" height="60" fill="#f0f0f0" rx="8"/>';

    // 干扰线
    for (let i = 0; i < 3; i++) {
      const x1 = Math.random() * 140;
      const y1 = Math.random() * 50;
      const x2 = x1 + Math.random() * 40;
      const y2 = y1 + Math.random() * 40;
      svg += `<line x1="${x1}" y1="${y1}" x2="${x2}" y2="${y2}" stroke="${colors[i % colors.length]}" stroke-width="1.5" opacity="0.5"/>`;
    }

    // 干扰点
    for (let i = 0; i < 15; i++) {
      const cx = Math.random() * 150;
      const cy = Math.random() * 50;
      svg += `<circle cx="${cx}" cy="${cy}" r="2" fill="${colors[i % colors.length]}" opacity="0.4"/>`;
    }

    // 文字
    chars.forEach((char, i) => {
      const x = 20 + i * 32;
      const y = 35 + Math.random() * 10;
      const rotation = (Math.random() - 0.5) * 30;
      const fontSize = 24 + Math.random() * 8;
      const color = colors[i % colors.length];
      svg += `<text x="${x}" y="${y}" font-size="${fontSize}" fill="${color}" 
        transform="rotate(${rotation}, ${x}, ${y})" font-weight="bold" 
        font-family="Arial, sans-serif">${char}</text>`;
    });

    svg += '</svg>';
    return svg;
  }
}