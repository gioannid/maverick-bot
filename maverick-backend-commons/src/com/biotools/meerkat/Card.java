package com.biotools.meerkat;

import java.io.Serializable;

public final class Card implements Serializable
{
	private static final long serialVersionUID = -2513909431724003234L;

	public static final int SPADES = 0;
	public static final int HEARTS = 1;
	public static final int DIAMONDS = 2;
	public static final int CLUBS = 3;
	public static final int BAD_CARD = -1;
	public static final int TWO = 0;
	public static final int THREE = 1;
	public static final int FOUR = 2;
	public static final int FIVE = 3;
	public static final int SIX = 4;
	public static final int SEVEN = 5;
	public static final int EIGHT = 6;
	public static final int NINE = 7;
	public static final int TEN = 8;
	public static final int JACK = 9;
	public static final int QUEEN = 10;
	public static final int KING = 11;
	public static final int ACE = 12;
	public static final int NUM_SUITS = 4;
	public static final int NUM_RANKS = 13;
	public static final int NUM_CARDS = 52;

	private static final char[] suits = { 's', 'h', 'd', 'c' };
	private static final char[] fancySuits = { '♠', '♥', '♦', '♣' };
	private static final char[] ranks = { '2', '3', '4', '5', '6', '7', '8', '9', 'T', 'J', 'Q', 'K', 'A' };

	public static boolean useTwoCharsFor10 = true;

	private static boolean useFancySuits;
	private static boolean initialized = false;
	private static final Card[] canonicalCards = new Card[52];
	
	private int index;
	private boolean mutable = true;

	static {
		for (int i = 0; i < 52; i++) {
			canonicalCards[i] = new Card(i);
			canonicalCards[i].mutable = false;
		}
	}
	
	public static void setUseSuitSymbols(boolean paramBoolean)
	{
		useFancySuits = paramBoolean;
	}
	
	public static Card get(int i) {
		return canonicalCards[i];
	}
	
	public static Card get(int rank, int suit) {
		return canonicalCards[13*suit+rank];
	}


	public Card()
	{
		this.index = -1;
		init();
	}

	public Card(int paramInt1, int paramInt2)
	{
		this.index = toIndex(paramInt1, paramInt2);
		init();
	}

	public Card(int paramInt)
	{
		if ((paramInt >= 0) && (paramInt < 52))
			this.index = paramInt;
		else
			this.index = -1;
		init();
	}

	public Card(String paramString)
	{
		if (paramString.length() == 2)
			this.index = A(paramString.charAt(0), paramString.charAt(1));
		else if ((paramString.length() == 3) && (paramString.startsWith("10")))
			this.index = toIndex(8, getSuitFromChar(paramString.charAt(2)));
		init();
	}
	
	public Card(char paramChar1, char paramChar2)
	{
		this.index = A(paramChar1, paramChar2);
	}



	public void init()
	{
		if (!initialized)
		{
			initialized = true;
			setUseSuitSymbols(false);
		}
	}

	public boolean equals(Card paramCard)
	{
		if (paramCard == null)
			return false;
		return paramCard.index == this.index;
	}

	private int A(char paramChar1, char paramChar2)
	{
		int i = -1;
		i = getRankFromChar(Character.toUpperCase(paramChar1));
		int j = -1;
		j = getSuitFromChar(Character.toLowerCase(paramChar2));
		if ((j != -1) && (i != -1))
			return toIndex(i, j);
		return -1;
	}

	public final int getIndex()
	{
		return this.index;
	}

	public void setIndex(int paramInt)
	{
		if (! mutable)
			throw new RuntimeException ("Card.setIndex(): is not mutable");
		this.index = paramInt;
	}

	public static final int toIndex(int paramInt1, int paramInt2)
	{
		return 13 * paramInt2 + paramInt1;
	}

	public void setCard(int paramInt1, int paramInt2)
	{
		if (! mutable)
			throw new RuntimeException ("Card.setCard(): is not mutable");
		this.index = toIndex(paramInt1, paramInt2);
	}

	public final int getRank()
	{
		return this.index % 13;
	}

	public static final int getRank(int paramInt)
	{
		return paramInt % 13;
	}

	public static final int getSuit(int paramInt)
	{
		return paramInt / 13;
	}

	public final int getSuit()
	{
		return this.index / 13;
	}

	public String toString()
	{
		return toEnglishString();
	}

	public String toTranslatedString()
	{
		if (this.index < 0)
			return "??";
		StringBuffer localStringBuffer = new StringBuffer(2);
		int i = getRank();
		if ((i == 8) && (useTwoCharsFor10))
			localStringBuffer.append("10");
		else
			localStringBuffer.append(getRankChar(i));
		localStringBuffer.append(getSuitChar(getSuit()));
		return localStringBuffer.toString();
	}

	public String toEnglishString()
	{
		if (this.index < 0)
			return "??";
		StringBuffer localStringBuffer = new StringBuffer(2);
		localStringBuffer.append(getEnglishRankChar(getRank()));
		localStringBuffer.append(getEnglishSuitChar(getSuit()));
		return localStringBuffer.toString();
	}

	public static int getRankFromChar(char paramChar)
	{
		char[] arrayOfChar = ranks;
		for (int i = 0; i < arrayOfChar.length; i++)
		{
			if (arrayOfChar[i] == paramChar)
				return i;
			if (Character.toLowerCase(arrayOfChar[i]) == paramChar)
				return i;
		}
		return -1;
	}

	public static int getSuitFromChar(char paramChar)
	{
		char[] arrayOfChar = suits;
		for (int i = 0; i < arrayOfChar.length; i++)
		{
			if (arrayOfChar[i] == paramChar)
				return i;
			if (Character.toLowerCase(arrayOfChar[i]) == paramChar)
				return i;
		}
		arrayOfChar = fancySuits;
		for (int i = 0; i < arrayOfChar.length; i++)
		{
			if (arrayOfChar[i] == paramChar)
				return i;
			if (Character.toLowerCase(arrayOfChar[i]) == paramChar)
				return i;
		}
		return -1;
	}

	public static char getRankChar(int paramInt)
	{
		char[] arrayOfChar = ranks;
		if ((paramInt >= 0) && (paramInt < 13))
			return arrayOfChar[paramInt];
		return '*';
	}

	public static String getExpandedRankString(int paramInt)
	{
		char[] arrayOfChar = ranks;
		if ((paramInt == 8) && (useTwoCharsFor10))
			return "10";
		if ((paramInt >= 0) && (paramInt < 13))
			return String.valueOf(arrayOfChar[paramInt]);
		return "*";
	}

	public static char getSuitChar(int paramInt)
	{
		char[] arrayOfChar = useFancySuits ? fancySuits : suits;
		if ((paramInt >= 0) && (paramInt < 4))
			return arrayOfChar[paramInt];
		return '*';
	}

	public static char getEnglishRankChar(int paramInt)
	{
		char[] arrayOfChar = ranks;
		if ((paramInt >= 0) && (paramInt < 13))
			return arrayOfChar[paramInt];
		return '*';
	}

	public static char getEnglishSuitChar(int paramInt)
	{
		char[] arrayOfChar = suits;
		if ((paramInt >= 0) && (paramInt < 4))
			return arrayOfChar[paramInt];
		return '*';
	}

	public boolean valid()
	{
		return (this.index >= 0) && (this.index < 52);
	}
}